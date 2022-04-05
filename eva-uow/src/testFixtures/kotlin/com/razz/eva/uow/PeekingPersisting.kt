package com.razz.eva.uow

import com.razz.eva.domain.Model
import com.razz.eva.domain.ModelId

internal class PeekingPersisting : ModelPersisting {

    private var currentModel: Model<*, *>? = null

    override fun <ID : ModelId<out Comparable<*>>, M : Model<ID, *>> add(model: M) {
        require(currentModel == null)
        currentModel = model
    }

    override fun <ID : ModelId<out Comparable<*>>, M : Model<ID, *>> update(model: M) {
        require(currentModel == null)
        currentModel = model
    }

    fun peek(): Model<*, *> {
        val model = requireNotNull(currentModel)
        currentModel = null
        return model
    }
}
