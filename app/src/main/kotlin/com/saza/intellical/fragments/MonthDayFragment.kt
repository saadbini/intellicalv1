package com.saza.intellical.fragments

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.saza.intellical.activities.MainActivity
import com.saza.intellical.activities.SimpleActivity
import com.saza.intellical.adapters.EventListAdapter
import com.saza.intellical.databinding.FragmentMonthDayBinding
import com.saza.intellical.extensions.*
import com.saza.intellical.helpers.Config
import com.saza.intellical.helpers.DAY_CODE
import com.saza.intellical.helpers.Formatter
import com.saza.intellical.helpers.Formatter.YEAR_PATTERN
import com.saza.intellical.helpers.MonthlyCalendarImpl
import com.saza.intellical.interfaces.MonthlyCalendar
import com.saza.intellical.interfaces.NavigationListener
import com.saza.intellical.models.DayMonthly
import com.saza.intellical.models.Event
import com.saza.intellical.models.ListEvent
import com.saza.commons.extensions.areSystemAnimationsEnabled
import com.saza.commons.extensions.beVisibleIf
import com.saza.commons.extensions.getProperTextColor
import com.saza.commons.interfaces.RefreshRecyclerViewListener
import org.joda.time.DateTime

class MonthDayFragment : Fragment(), MonthlyCalendar, RefreshRecyclerViewListener {
    private var mShowWeekNumbers = false
    private var mDayCode = ""
    private var mSelectedDayCode = ""
    private var mPackageName = ""
    private var mLastHash = 0L
    private var mCalendar: MonthlyCalendarImpl? = null
    private var mListEvents = ArrayList<Event>()

    var listener: NavigationListener? = null

    private lateinit var mRes: Resources
    private lateinit var binding: FragmentMonthDayBinding
    private lateinit var mConfig: Config

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMonthDayBinding.inflate(inflater, container, false)
        mRes = resources
        mPackageName = requireActivity().packageName
        mDayCode = requireArguments().getString(DAY_CODE)!!

        val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
        binding.monthDaySelectedDayLabel.apply {
            text = getMonthLabel(shownMonthDateTime)
            setOnClickListener {
                (activity as MainActivity).showGoToDateDialog()
            }
        }

        mConfig = requireContext().config
        storeStateVariables()
        setupButtons()
        mCalendar = MonthlyCalendarImpl(this, requireContext())
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
    }

    override fun onResume() {
        super.onResume()
        if (mConfig.showWeekNumbers != mShowWeekNumbers) {
            mLastHash = -1L
        }

        mCalendar!!.apply {
            mTargetDate = Formatter.getDateTimeFromCode(mDayCode)
            getDays(false)    // prefill the screen asap, even if without events
        }

        storeStateVariables()
        updateCalendar()
    }

    private fun storeStateVariables() {
        mConfig.apply {
            mShowWeekNumbers = showWeekNumbers
        }
    }

    fun updateCalendar() {
        mCalendar?.updateMonthlyCalendar(Formatter.getDateTimeFromCode(mDayCode))
    }

    override fun updateMonthlyCalendar(context: Context, month: String, days: ArrayList<DayMonthly>, checkedEvents: Boolean, currTargetDate: DateTime) {
        val newHash = month.hashCode() + days.hashCode().toLong()
        if ((mLastHash != 0L && !checkedEvents) || mLastHash == newHash) {
            return
        }

        mLastHash = newHash

        activity?.runOnUiThread {
            binding.monthDayViewWrapper.updateDays(days, false) {
                mSelectedDayCode = it.code
                updateVisibleEvents()
            }
        }

        refreshItems()
    }

    private fun updateVisibleEvents() {
        if (activity == null) {
            return
        }

        val filtered = mListEvents.filter {
            if (mSelectedDayCode.isEmpty()) {
                val shownMonthDateTime = Formatter.getDateTimeFromCode(mDayCode)
                val startDateTime = Formatter.getDateTimeFromTS(it.startTS)
                shownMonthDateTime.year == startDateTime.year && shownMonthDateTime.monthOfYear == startDateTime.monthOfYear
            } else {
                val selectionDate = Formatter.getDateTimeFromCode(mSelectedDayCode).toLocalDate()
                val startDate = Formatter.getDateFromTS(it.startTS)
                val endDate = Formatter.getDateFromTS(it.endTS)
                selectionDate in startDate..endDate
            }
        }

        val listItems = requireActivity().getEventListItems(filtered, mSelectedDayCode.isEmpty(), false)
        if (mSelectedDayCode.isNotEmpty()) {
            binding.monthDaySelectedDayLabel.text = Formatter.getDateFromCode(requireActivity(), mSelectedDayCode, false)
        }

        activity?.runOnUiThread {
            if (activity != null) {
                binding.monthDayEventsList.beVisibleIf(listItems.isNotEmpty())
                binding.monthDayNoEventsPlaceholder.beVisibleIf(listItems.isEmpty())

                val currAdapter = binding.monthDayEventsList.adapter
                if (currAdapter == null) {
                    EventListAdapter(activity as SimpleActivity, listItems, true, this, binding.monthDayEventsList) {
                        if (it is ListEvent) {
                            activity?.editEvent(it)
                        }
                    }.apply {
                        binding.monthDayEventsList.adapter = this
                    }

                    if (requireContext().areSystemAnimationsEnabled) {
                        binding.monthDayEventsList.scheduleLayoutAnimation()
                    }
                } else {
                    (currAdapter as EventListAdapter).updateListItems(listItems)
                }
            }
        }
    }

    private fun setupButtons() {
        val textColor = requireContext().getProperTextColor()
        binding.apply {
            monthDaySelectedDayLabel.setTextColor(textColor)
            monthDayNoEventsPlaceholder.setTextColor(textColor)
        }
    }

    fun printCurrentView() {}

    fun getNewEventDayCode() = mSelectedDayCode.ifEmpty { null }

    private fun getMonthLabel(shownMonthDateTime: DateTime): String {
        var month = Formatter.getMonthName(requireActivity(), shownMonthDateTime.monthOfYear)
        val targetYear = shownMonthDateTime.toString(YEAR_PATTERN)
        if (targetYear != DateTime().toString(YEAR_PATTERN)) {
            month += " $targetYear"
        }
        return month
    }

    override fun refreshItems() {
        val startDateTime = Formatter.getLocalDateTimeFromCode(mDayCode).minusWeeks(1)
        val endDateTime = startDateTime.plusWeeks(7)
        activity?.eventsHelper?.getEvents(startDateTime.seconds(), endDateTime.seconds()) { events ->
            mListEvents = events
            activity?.runOnUiThread {
                updateVisibleEvents()
            }
        }
    }
}
