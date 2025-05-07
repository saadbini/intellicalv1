package com.saza.intellical.adapters

import android.view.*
import android.widget.PopupMenu
import com.saza.intellical.R
import com.saza.intellical.activities.SimpleActivity
import com.saza.intellical.databinding.ItemEventTypeBinding
import com.saza.intellical.extensions.eventsHelper
import com.saza.intellical.helpers.REGULAR_EVENT_TYPE_ID
import com.saza.intellical.interfaces.DeleteEventTypesListener
import com.saza.intellical.models.EventType
import com.saza.commons.adapters.MyRecyclerViewAdapter
import com.saza.commons.dialogs.ConfirmationDialog
import com.saza.commons.dialogs.RadioGroupDialog
import com.saza.commons.extensions.*
import com.saza.commons.models.RadioItem
import com.saza.commons.views.MyRecyclerView

class ManageEventTypesAdapter(
    activity: SimpleActivity, val eventTypes: ArrayList<EventType>, val listener: DeleteEventTypesListener?, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    private val MOVE_EVENTS = 0
    private val DELETE_EVENTS = 1

    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_event_type

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_edit).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_edit -> editEventType()
            R.id.cab_delete -> askConfirmDelete()
        }
    }

    override fun getSelectableItemCount() = eventTypes.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = eventTypes.getOrNull(position)?.id?.toInt()

    override fun getItemKeyPosition(key: Int) = eventTypes.indexOfFirst { it.id?.toInt() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(
            view = ItemEventTypeBinding.inflate(activity.layoutInflater, parent, false).root
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val eventType = eventTypes[position]
        holder.bindView(eventType, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, eventType)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = eventTypes.size

    private fun getItemWithKey(key: Int): EventType? = eventTypes.firstOrNull { it.id?.toInt() == key }

    private fun getSelectedItems() = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) } as ArrayList<EventType>

    private fun setupView(view: View, eventType: EventType) {
        ItemEventTypeBinding.bind(view).apply {
            eventItemFrame.isSelected = selectedKeys.contains(eventType.id?.toInt())
            eventTypeTitle.text = eventType.getDisplayTitle()
            eventTypeColor.setFillWithStroke(eventType.color, activity.getProperBackgroundColor())
            eventTypeTitle.setTextColor(textColor)

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, eventType)
            }
        }
    }

    private fun showPopupMenu(view: View, eventType: EventType) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val eventTypeId = eventType.id!!.toInt()
                when (item.itemId) {
                    R.id.cab_edit -> {
                        executeItemMenuOperation(eventTypeId) {
                            itemClick(eventType)
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(eventTypeId) {
                            askConfirmDelete()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(eventTypeId: Int, callback: () -> Unit) {
        selectedKeys.clear()
        selectedKeys.add(eventTypeId)
        callback()
    }

    private fun editEventType() {
        itemClick.invoke(getSelectedItems().first())
        finishActMode()
    }

    private fun askConfirmDelete() {
        val eventTypes = eventTypes.filter { selectedKeys.contains(it.id?.toInt()) }.map { it.id } as ArrayList<Long>

        activity.eventsHelper.doEventTypesContainEvents(eventTypes) {
            activity.runOnUiThread {
                if (it) {
                    val res = activity.resources
                    val items = ArrayList<RadioItem>().apply {
                        add(RadioItem(MOVE_EVENTS, res.getString(R.string.move_events_into_default)))
                        add(RadioItem(DELETE_EVENTS, res.getString(R.string.remove_affected_events)))
                    }
                    RadioGroupDialog(activity, items) {
                        deleteEventTypes(it == DELETE_EVENTS)
                    }
                } else {
                    ConfirmationDialog(activity) {
                        deleteEventTypes(true)
                    }
                }
            }
        }
    }

    private fun deleteEventTypes(deleteEvents: Boolean) {
        val eventTypesToDelete = getSelectedItems()

        for (key in selectedKeys) {
            val type = getItemWithKey(key) ?: continue
            if (type.id == REGULAR_EVENT_TYPE_ID) {
                activity.toast(R.string.cannot_delete_default_type)
                eventTypesToDelete.remove(type)
                toggleItemSelection(false, getItemKeyPosition(type.id!!.toInt()))
                break
            }
        }

        if (listener?.deleteEventTypes(eventTypesToDelete, deleteEvents) == true) {
            val positions = getSelectedItemPositions()
            eventTypes.removeAll(eventTypesToDelete)
            removeSelectedItems(positions)
        }
    }
}
