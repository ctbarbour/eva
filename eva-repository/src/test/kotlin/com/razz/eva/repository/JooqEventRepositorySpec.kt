package com.razz.eva.repository

import com.razz.eva.IdempotencyKey
import com.razz.eva.domain.DepartmentEvent.OrphanedDepartmentCreated
import com.razz.eva.domain.DepartmentId.Companion.randomDepartmentId
import com.razz.eva.domain.EmployeeEvent.EmailChanged
import com.razz.eva.domain.EmployeeEvent.EmployeeCreated
import com.razz.eva.domain.EmployeeId.Companion.randomEmployeeId
import com.razz.eva.domain.Name
import com.razz.eva.domain.Principal
import com.razz.eva.domain.Principal.Id
import com.razz.eva.domain.Ration
import com.razz.eva.events.UowEvent
import com.razz.eva.events.UowEvent.ModelEventId
import com.razz.eva.events.UowEvent.UowName
import com.razz.eva.events.db.tables.ModelEvents.MODEL_EVENTS
import com.razz.eva.events.db.tables.UowEvents.UOW_EVENTS
import com.razz.eva.events.db.tables.records.ModelEventsRecord
import com.razz.eva.events.db.tables.records.UowEventsRecord
import com.razz.eva.persistence.executor.FakeMemorizingQueryExecutor
import com.razz.eva.persistence.executor.FakeMemorizingQueryExecutor.ExecutionStep.QueryExecuted
import com.razz.eva.serialization.json.JsonFormat.json
import com.razz.eva.uow.UowParams
import com.razz.eva.tracing.Tracing.notReportingTracer
import com.razz.eva.tracing.Tracing.withNewSpan
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.parseToJsonElement
import org.jooq.SQLDialect.POSTGRES
import org.jooq.impl.DSL
import java.time.Instant.now
import java.util.UUID.randomUUID

@Serializable
data class Params(
    val id: Int,
    val name: String,
    override val idempotencyKey: IdempotencyKey
) : UowParams<Params> {
    override fun serialization() = serializer()
}

class JooqEventRepositorySpec : BehaviorSpec({

    val now = now()
    lateinit var spanId: String
    val inner = notReportingTracer()
    val tracer = object : Tracer by inner {
        override fun <C> inject(spanContext: SpanContext, format: Format<C>, carrier: C) {
            spanId = spanContext.toSpanId()
            inner.inject(spanContext, format, carrier)
        }
    }
    val anotherPrincipal = object : Principal<String> {
        override val id = Id("ANOTHER_ID")
        override val name = Principal.Name("ANOTHER_PRINCIPAL")
    }

    Given("SqlEventRepository with hacked queryExecutor and tracing context") {
        val dslContext = DSL.using(POSTGRES)
        val queryExecutor = FakeMemorizingQueryExecutor()

        val eventRepo = JooqEventRepository(queryExecutor, dslContext, tracer)

        And("Unit of Work Event") {
            val depId = randomDepartmentId()
            val empId = randomEmployeeId()
            val params = Params(1, "Nik", IdempotencyKey.random())
            val uowEvent = UowEvent(
                id = UowEvent.Id.random(),
                uowName = UowName("TestUow"),
                principal = TestPrincipal,
                modelEvents = listOf(
                    ModelEventId.random() to OrphanedDepartmentCreated(
                        depId,
                        "PoContrE",
                        1_337,
                        Ration.SHAKSHOUKA
                    ),
                    ModelEventId.random() to EmployeeCreated(
                        empId,
                        Name("rabotyaga", "#1"),
                        depId,
                        "rabotyaga1@top_pocontre.eu",
                        Ration.SHAKSHOUKA
                    ),
                    ModelEventId.random() to EmailChanged(
                        empId,
                        "old@email.com",
                        "new@email.com",
                        anotherPrincipal
                    ),
                ),
                idempotencyKey = params.idempotencyKey,
                params = json.encodeToString(params.serialization(), params),
                occurredAt = now
            )

            When("Principal saving Unit of Work Event with two model events") {
                withNewSpan(
                    tracer,
                    {
                        it.buildSpan("event-repo-spec").asChildOf(
                            tracer.extract(
                                Format.Builtin.TEXT_MAP,
                                TextMapAdapter(
                                    mapOf(
                                        "x-b3-spanid" to "0000000001234567",
                                        "x-b3-traceid" to "0000000007654321",
                                        "x-b3-sampled" to "1"
                                    )
                                )
                            )
                        ).start()
                    }
                ) {
                    eventRepo.add(uowEvent)
                }

                Then("Query executor should receive one uow event and two model events") {
                    queryExecutor.executionHistory shouldBe listOf(
                        QueryExecuted(
                            dslContext,
                            dslContext.insertQuery(UOW_EVENTS)
                                .also {
                                    it.setRecord(
                                        UowEventsRecord().apply {
                                            this.id = uowEvent.id.uuidValue()
                                            this.name = uowEvent.uowName.toString()
                                            this.idempotencyKey = uowEvent.idempotencyKey?.stringValue()
                                            this.principalName = "TEST_PRINCIPAL"
                                            this.principalId = "THIS_IS_SINGLETON"
                                            this.occurredAt = now
                                            this.modelEvents = uowEvent
                                                .modelEvents.map { (id, _) -> id.uuidValue() }.toTypedArray()
                                            this.params = parseToJsonElement(
                                                """
                                                {
                                                    "id":1,
                                                    "name":"Nik",
                                                    "idempotencyKey":"${uowEvent.idempotencyKey?.stringValue()}"
                                                }
                                                """
                                            ).toString()
                                        }
                                    )
                                },
                        ),
                        QueryExecuted(
                            dslContext,
                            dslContext.insertQuery(MODEL_EVENTS)
                                .also {
                                    it.addRecord(
                                        uowEvent.modelEvents[0].let { (key, value) ->
                                            ModelEventsRecord().apply {
                                                this.id = key.uuidValue()
                                                this.uowId = uowEvent.id.uuidValue()
                                                this.modelId = value.modelId.id.toString()
                                                this.name = value.eventName()
                                                this.modelName = value.modelName
                                                this.occurredAt = now
                                                this.payload = json.parseToJsonElement("""
                                                        {
                                                            "principalId":"THIS_IS_SINGLETON",
                                                            "principalName":"TEST_PRINCIPAL",
                                                            "name":"PoContrE",
                                                            "headcount":1337,
                                                            "ration":"SHAKSHOUKA"
                                                        }
                                                    """).toString()
                                                this.tracingContext = parseToJsonElement(
                                                    """
                                                    {
                                                        "X-B3-TraceId":"0000000007654321",
                                                        "X-B3-ParentSpanId":"0000000001234567",
                                                        "X-B3-SpanId":"$spanId",
                                                        "X-B3-Sampled":"1"
                                                    }
                                                    """
                                                ).toString()
                                            }
                                        }
                                    )
                                    it.addRecord(
                                        uowEvent.modelEvents[1].let { (key, value) ->
                                            ModelEventsRecord().apply {
                                                this.id = key.uuidValue()
                                                this.uowId = uowEvent.id.uuidValue()
                                                this.modelId = value.modelId.id.toString()
                                                this.name = value.eventName()
                                                this.modelName = value.modelName
                                                this.occurredAt = now
                                                this.payload = json.parseToJsonElement("""
                                                        {
                                                            "employeeId":"${empId.id}",
                                                            "name":{"first":"rabotyaga","last":"#1"},
                                                            "departmentId":"${depId.id}",
                                                            "email":"rabotyaga1@top_pocontre.eu",
                                                            "ration":"SHAKSHOUKA"
                                                        }
                                                    """).toString()
                                                this.tracingContext = parseToJsonElement(
                                                    """
                                                    {
                                                        "X-B3-TraceId":"0000000007654321",
                                                        "X-B3-ParentSpanId":"0000000001234567",
                                                        "X-B3-SpanId":"$spanId",
                                                        "X-B3-Sampled":"1"
                                                    }
                                                    """
                                                ).toString()
                                            }
                                        }
                                    )
                                    it.addRecord(
                                        uowEvent.modelEvents[2].let { (key, value) ->
                                            ModelEventsRecord().apply {
                                                this.id = key.uuidValue()
                                                this.uowId = uowEvent.id.uuidValue()
                                                this.modelId = value.modelId.id.toString()
                                                this.name = value.eventName()
                                                this.modelName = value.modelName
                                                this.occurredAt = now
                                                this.payload = json.parseToJsonElement("""
                                                        {
                                                            "principalId":"ANOTHER_ID",
                                                            "principalName":"TEST_PRINCIPAL",
                                                            "oldEmail":"old@email.com",
                                                            "newEmail":"new@email.com"
                                                        }
                                                    """).toString()
                                                this.tracingContext = parseToJsonElement(
                                                    """
                                                    {
                                                        "X-B3-TraceId":"0000000007654321",
                                                        "X-B3-ParentSpanId":"0000000001234567",
                                                        "X-B3-SpanId":"$spanId",
                                                        "X-B3-Sampled":"1"
                                                    }
                                                    """
                                                ).toString()
                                            }
                                        }
                                    )
                                },
                        )
                    )
                }
            }
        }
    }

    Given("Another sqlEventRepository with hacked queryExecutor and tracing context") {
        val dslContext = DSL.using(POSTGRES)
        val queryExecutor = FakeMemorizingQueryExecutor()

        val eventRepo = JooqEventRepository(queryExecutor, dslContext, tracer)

        And("Unit of Work Event without model events") {
            val params = Params(1, "Nik", IdempotencyKey.random())
            val uowEvent = UowEvent(
                id = UowEvent.Id(randomUUID()),
                uowName = UowName("TestUow"),
                principal = TestPrincipal,
                modelEvents = listOf(),
                idempotencyKey = params.idempotencyKey,
                params = json.encodeToString(params.serialization(), params),
                occurredAt = now
            )

            When("Principal saving Unit of Work Event") {
                withNewSpan(
                    tracer,
                    {
                        it.buildSpan("event-repo-spec").asChildOf(
                            tracer.extract(
                                Format.Builtin.TEXT_MAP,
                                TextMapAdapter(
                                    mapOf(
                                        "x-b3-spanid" to "0000000001234567",
                                        "x-b3-traceid" to "0000000007654321",
                                        "x-b3-sampled" to "1"
                                    )
                                )
                            )
                        ).start()
                    }
                ) {
                    eventRepo.add(uowEvent)
                }

                Then("Query executor should receive one uow event") {
                    queryExecutor.executionHistory shouldBe listOf(
                        QueryExecuted(
                            dslContext,
                            dslContext.insertQuery(UOW_EVENTS)
                                .also {
                                    it.setRecord(
                                        UowEventsRecord().apply {
                                            this.id = uowEvent.id.uuidValue()
                                            this.name = uowEvent.uowName.toString()
                                            this.idempotencyKey = uowEvent.idempotencyKey?.stringValue()
                                            this.principalName = "TEST_PRINCIPAL"
                                            this.principalId = "THIS_IS_SINGLETON"
                                            this.occurredAt = now
                                            this.modelEvents = uowEvent
                                                .modelEvents.map { (id, _) -> id.uuidValue() }.toTypedArray()
                                            this.params = parseToJsonElement(
                                                """
                                                {
                                                    "id":1,
                                                    "name":"Nik",
                                                    "idempotencyKey":"${uowEvent.idempotencyKey?.stringValue()}"
                                                }
                                                """
                                            ).toString()
                                        }
                                    )
                                },
                        )
                    )
                }
            }
        }
    }

    Given("Another sqlEventRepository with hacked queryExecutor and no tracing context") {
        val dslContext = DSL.using(POSTGRES)
        val queryExecutor = FakeMemorizingQueryExecutor()

        val eventRepo = JooqEventRepository(queryExecutor, dslContext, tracer)

        And("Unit of Work Event without model events") {
            val params = Params(1, "Nik", IdempotencyKey.random())
            val uowEvent = UowEvent(
                id = UowEvent.Id(randomUUID()),
                uowName = UowName("TestUow"),
                principal = TestPrincipal,
                modelEvents = listOf(),
                idempotencyKey = params.idempotencyKey,
                params = json.encodeToString(params.serialization(), params),
                occurredAt = now
            )

            When("Principal saving Unit of Work Event") {
                eventRepo.add(uowEvent)

                Then("Query executor should receive one uow event") {
                    queryExecutor.executionHistory shouldBe listOf(
                        QueryExecuted(
                            dslContext,
                            dslContext.insertQuery(UOW_EVENTS)
                                .also {
                                    it.setRecord(
                                        UowEventsRecord().apply {
                                            id = uowEvent.id.uuidValue()
                                            name = uowEvent.uowName.toString()
                                            idempotencyKey = uowEvent.idempotencyKey?.stringValue()
                                            principalName = "TEST_PRINCIPAL"
                                            principalId = "THIS_IS_SINGLETON"
                                            occurredAt = now
                                            modelEvents = uowEvent
                                                .modelEvents.map { (id, _) -> id.uuidValue() }.toTypedArray()
                                            this.params = parseToJsonElement(
                                                """
                                                {
                                                    "id":1,
                                                    "name":"Nik",
                                                    "idempotencyKey":"${uowEvent.idempotencyKey?.stringValue()}"
                                                }
                                                """
                                            ).toString()
                                        }
                                    )
                                },
                        )
                    )
                }
            }
        }
    }
})
