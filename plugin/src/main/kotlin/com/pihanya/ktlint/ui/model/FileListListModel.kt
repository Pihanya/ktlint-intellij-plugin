package com.pihanya.ktlint.ui.model

import com.intellij.openapi.vfs.VirtualFile
import javax.swing.AbstractListModel

@Suppress("TooManyFunctions") // Required functionality
class FileListListModel(initialItems: List<VirtualFile> = emptyList()) : AbstractListModel<VirtualFile>() {

    private val _items: MutableList<VirtualFile> = initialItems.toMutableList()

    val items: List<VirtualFile> = _items

    override fun getSize() = items.size

    override fun getElementAt(index: Int) = _items[index]

    operator fun get(index: Int) = _items[index]

    operator fun set(index: Int, value: VirtualFile) {
        _items[index] = value
        fireContentsChanged(this, index, index)
    }

    operator fun plusAssign(newItem: VirtualFile) {
        add(newItem)
    }

    operator fun plusAssign(newItems: Collection<VirtualFile>) {
        addAll(newItems)
    }

    fun add(newItem: VirtualFile, index: Int = _items.size) {
        _items.add(index, newItem)
        fireIntervalAdded(this, index, index)
    }

    fun addAll(newItems: Collection<VirtualFile>, index: Int = _items.size) {
        if (newItems.isEmpty()) return
        _items.addAll(index, newItems)
        fireIntervalAdded(this, index, index + newItems.size - 1)
    }

    fun removeAt(indices: Collection<Int>): List<VirtualFile> {
        if (indices.isEmpty()) return emptyList()
        val removed = indices.reversed()
            .map { indexToRemove ->
                val removed = _items.removeAt(indexToRemove)
                fireIntervalRemoved(this, indexToRemove, indexToRemove)
                removed
            }
        return removed
    }

    fun clear() {
        val formerSize = items.size
        _items.clear()
        fireIntervalRemoved(this, 0, formerSize)
    }
}
