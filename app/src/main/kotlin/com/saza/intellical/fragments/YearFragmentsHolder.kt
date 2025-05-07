package com.saza.intellical.fragments

import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.viewpager.widget.ViewPager
import com.saza.intellical.activities.MainActivity
import com.saza.intellical.adapters.MyYearPagerAdapter
import com.saza.intellical.databinding.FragmentYearsHolderBinding
import com.saza.intellical.helpers.Formatter
import com.saza.intellical.helpers.YEARLY_VIEW
import com.saza.intellical.helpers.YEAR_TO_OPEN
import com.saza.intellical.interfaces.NavigationListener
import com.saza.commons.extensions.beGone
import com.saza.commons.extensions.getAlertDialogBuilder
import com.saza.commons.extensions.getProperBackgroundColor
import com.saza.commons.extensions.setupDialogStuff
import com.saza.commons.views.MyViewPager
import com.saza.intellical.R
import org.joda.time.DateTime

class YearFragmentsHolder : MyFragmentHolder(), NavigationListener {
    private val PREFILLED_YEARS = 61

    private lateinit var viewPager: MyViewPager
    private var defaultYearlyPage = 0
    private var todayYear = 0
    private var currentYear = 0
    private var isGoToTodayVisible = false

    override val viewType = YEARLY_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dateTimeString = arguments?.getString(YEAR_TO_OPEN)
        currentYear = (if (dateTimeString != null) DateTime.parse(dateTimeString) else DateTime()).toString(Formatter.YEAR_PATTERN).toInt()
        todayYear = DateTime().toString(Formatter.YEAR_PATTERN).toInt()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentYearsHolderBinding.inflate(inflater, container, false)
        binding.root.background = ColorDrawable(requireContext().getProperBackgroundColor())
        viewPager = binding.fragmentYearsViewpager
        viewPager.id = (System.currentTimeMillis() % 100000).toInt()
        setupFragment()
        return binding.root
    }

    private fun setupFragment() {
        val years = getYears(currentYear)
        val yearlyAdapter = MyYearPagerAdapter(requireActivity().supportFragmentManager, years, this)
        defaultYearlyPage = years.size / 2

        viewPager.apply {
            adapter = yearlyAdapter
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {}

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

                override fun onPageSelected(position: Int) {
                    currentYear = years[position]
                    val shouldGoToTodayBeVisible = shouldGoToTodayBeVisible()
                    if (isGoToTodayVisible != shouldGoToTodayBeVisible) {
                        (activity as? MainActivity)?.toggleGoToTodayVisibility(shouldGoToTodayBeVisible)
                        isGoToTodayVisible = shouldGoToTodayBeVisible
                    }
                }
            })
            currentItem = defaultYearlyPage
        }
    }

    private fun getYears(targetYear: Int): List<Int> {
        val years = ArrayList<Int>(PREFILLED_YEARS)
        years += targetYear - PREFILLED_YEARS / 2..targetYear + PREFILLED_YEARS / 2
        return years
    }

    override fun goLeft() {
        viewPager.currentItem = viewPager.currentItem - 1
    }

    override fun goRight() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

    override fun goToDateTime(dateTime: DateTime) {}

    override fun goToToday() {
        currentYear = todayYear
        setupFragment()
    }

    override fun showGoToDateDialog() {
        if (activity == null) {
            return
        }

        val datePicker = getDatePickerView()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("day", "id", "android")).beGone()
        datePicker.findViewById<View>(Resources.getSystem().getIdentifier("month", "id", "android")).beGone()

        val dateTime = DateTime(Formatter.getDateTimeFromCode("${currentYear}0523").toString())
        datePicker.init(dateTime.year, dateTime.monthOfYear - 1, 1, null)

        activity?.getAlertDialogBuilder()!!
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> datePicked(datePicker) }
            .apply {
                activity?.setupDialogStuff(datePicker, this)
            }
    }

    private fun datePicked(datePicker: DatePicker) {
        val pickedYear = datePicker.year
        if (currentYear != pickedYear) {
            currentYear = datePicker.year
            setupFragment()
        }
    }

    override fun refreshEvents() {
        (viewPager.adapter as? MyYearPagerAdapter)?.updateCalendars(viewPager.currentItem)
    }

    override fun shouldGoToTodayBeVisible() = currentYear != todayYear

    override fun getNewEventDayCode() = Formatter.getTodayCode()

    override fun printView() {
        (viewPager.adapter as? MyYearPagerAdapter)?.printCurrentView(viewPager.currentItem)
    }

    override fun getCurrentDate() = null
}
