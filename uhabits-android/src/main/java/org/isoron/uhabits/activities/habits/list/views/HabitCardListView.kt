/*
 * Copyright (C) 2016-2021 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.BundleSavedState
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.dp
import javax.inject.Inject

class HabitCardListViewFactory
@Inject constructor(
    @ActivityContext val context: Context,
    val adapter: HabitCardListAdapter,
    val cardViewFactory: HabitCardViewFactory,
    val controller: Lazy<HabitCardListController>
) {
    fun create() = HabitCardListView(context, adapter, cardViewFactory, controller)
}

class HabitCardListView(
    @ActivityContext context: Context,
    private val adapter: HabitCardListAdapter,
    private val cardViewFactory: HabitCardViewFactory,
    private val controller: Lazy<HabitCardListController>
) : RecyclerView(context, null, R.attr.scrollableRecyclerViewStyle) {

    var checkmarkCount: Int = 0
    private var insetDecorationsAdded: Boolean = false

    var dataOffset: Int = 0
        set(value) {
            field = value
            attachedHolders
                .map { it.itemView as HabitCardView }
                .forEach { it.dataOffset = value }
        }

    private val attachedHolders = mutableListOf<HabitCardViewHolder>()
    private val touchHelper = ItemTouchHelper(TouchHelperCallback()).apply {
        attachToRecyclerView(this@HabitCardListView)
    }

    init {
        setHasFixedSize(true)
        isLongClickable = true
        layoutManager = LinearLayoutManager(context)
        applyBottomInset()
        super.setAdapter(adapter)
    }

    private fun applyBottomInset() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            if (insetDecorationsAdded) return@setOnApplyWindowInsetsListener insets
            insetDecorationsAdded = true
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            addItemDecoration(object : ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: State
                ) {
                    val itemCount = parent.adapter?.itemCount
                    if (parent.getChildAdapterPosition(view) == itemCount?.minus(1)) {
                        outRect.bottom = systemBarsInsets.bottom
                    }
                }
            })
            insets
        }
    }

    fun createHabitCardView(): HabitCardView {
        return cardViewFactory.create()
    }

    fun createSectionHeaderView() = SectionHeaderView(context)

    fun bindHeaderView(holder: SectionHeaderViewHolder, header: HabitCardListCache.ListItem.Header) {
        (holder.itemView as SectionHeaderView).bind(header)
    }

    fun bindCardView(
        holder: HabitCardViewHolder,
        habit: Habit,
        score: Double,
        checkmarks: IntArray,
        notes: Array<String>,
        selected: Boolean
    ): View {
        val cardView = holder.itemView as HabitCardView
        cardView.habit = habit
        cardView.isSelected = selected
        cardView.values = checkmarks
        cardView.buttonCount = checkmarkCount
        cardView.dataOffset = dataOffset
        cardView.score = score
        cardView.unit = habit.unit
        cardView.threshold = habit.targetValue
        cardView.notes = notes
        cardView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != NO_POSITION) controller.get().onItemClick(position)
        }
        cardView.setOnLongClickListener {
            val position = holder.adapterPosition
            if (position == NO_POSITION) return@setOnLongClickListener false
            controller.get().onItemLongClick(position)
            true
        }
        val detector = GestureDetector(context, CardViewGestureDetector(holder))
        cardView.setOnTouchListener { _, ev ->
            detector.onTouchEvent(ev)
            true
        }

        return cardView
    }

    fun attachCardView(holder: HabitCardViewHolder) {
        (holder.itemView as HabitCardView).dataOffset = dataOffset
        attachedHolders.add(holder)
    }

    fun detachCardView(holder: HabitCardViewHolder) {
        attachedHolders.remove(holder)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        adapter.onAttached()
    }

    override fun onDetachedFromWindow() {
        adapter.onDetached()
        super.onDetachedFromWindow()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is BundleSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        dataOffset = state.bundle!!.getInt("dataOffset")
        super.onRestoreInstanceState(state.superState)
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val bundle = Bundle().apply {
            putInt("dataOffset", dataOffset)
        }
        return BundleSavedState(superState, bundle)
    }

    interface Controller {
        fun drop(from: Int, to: Int) {}
        fun onReorderFinished(from: Habit, to: Habit) {}
        fun onItemClick(pos: Int) {}
        fun onItemLongClick(pos: Int) {}
        fun startDrag(position: Int) {}
    }

    private inner class CardViewGestureDetector(
        private val holder: HabitCardViewHolder
    ) : GestureDetector.SimpleOnGestureListener() {

        override fun onLongPress(e: MotionEvent) {
            if (adapter.isSortable && adapter.isSelectionEmpty) {
                touchHelper.startDrag(holder)
            } else {
                holder.itemView.performLongClick()
            }
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            holder.itemView.performClick()
            return true
        }
    }

    inner class TouchHelperCallback : ItemTouchHelper.Callback() {
        private var reorderSession: HabitCardListCache.ReorderSession? = null
        private var draggedViewHolder: ViewHolder? = null

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder
        ): Int {
            if (!adapter.isSortable || !adapter.isSelectionEmpty) return 0
            if (adapter.getItem(viewHolder.adapterPosition) == null) return 0
            return makeMovementFlags(UP or DOWN, 0)
        }

        override fun canDropOver(
            recyclerView: RecyclerView,
            current: ViewHolder,
            target: ViewHolder
        ): Boolean {
            val fromPos = current.adapterPosition
            val toPos = target.adapterPosition
            if (fromPos == NO_POSITION || toPos == NO_POSITION) return false
            if (adapter.getItem(toPos) == null) return false
            return adapter.isSameSection(fromPos, toPos)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            from: ViewHolder,
            to: ViewHolder
        ): Boolean {
            val fromPos = from.adapterPosition
            val toPos = to.adapterPosition
            if (fromPos == NO_POSITION || toPos == NO_POSITION) return false
            if (!canDropOver(recyclerView, from, to)) return false
            return reorderSession?.move(fromPos, toPos) == true
        }

        override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            finishReorder()
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                reorderSession = adapter.startReorder(viewHolder.adapterPosition) ?: return
                draggedViewHolder = viewHolder
                viewHolder.itemView.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .translationZ(dp(6f))
                    .setDuration(120)
                    .start()
                viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }

        private fun finishReorder() {
            val session = reorderSession ?: return
            val holder = draggedViewHolder
            reorderSession = null
            draggedViewHolder = null

            // clearView can arrive after the next drag has already started.
            val target = session.finish()
            if (target != null) {
                controller.get().onReorderFinished(session.habit, target)
            } else if (!session.hasMoved) {
                holder?.itemView?.performLongClick()
            }
        }

        override fun clearView(
            recyclerView: RecyclerView,
            viewHolder: ViewHolder
        ) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .translationZ(0f)
                .setDuration(120)
                .start()
        }

        override fun onSwiped(
            viewHolder: ViewHolder,
            direction: Int
        ) {
        }

        override fun isItemViewSwipeEnabled() = false
        override fun isLongPressDragEnabled() = false
    }
}
