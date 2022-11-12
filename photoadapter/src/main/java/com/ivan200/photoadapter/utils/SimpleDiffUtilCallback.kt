package com.ivan200.photoadapter.utils

import androidx.recyclerview.widget.DiffUtil

/**
 * Simple callback for [DiffUtil]
 *
 * @author ivan200
 * @since 11.09.2022
 */
class SimpleDiffUtilCallback<T, V : Comparable<V>>(
    private val oldList: List<T>,
    private val newList: List<T>,
    val getId: (T) -> V
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        getId(oldList[oldItemPosition]) == getId(newList[newItemPosition])

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldList[oldItemPosition] == newList[newItemPosition]
}