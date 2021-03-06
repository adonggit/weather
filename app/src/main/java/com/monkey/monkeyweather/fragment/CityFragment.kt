package com.monkey.monkeyweather.fragment


import `in`.srain.cube.views.ptr.PtrDefaultHandler
import `in`.srain.cube.views.ptr.PtrFrameLayout
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.LinearLayoutManager
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.monkey.monkeyweather.R
import com.monkey.monkeyweather.activity.AirQualityActivity
import com.monkey.monkeyweather.activity.FifteenForecastActivity
import com.monkey.monkeyweather.activity.MainActivity
import com.monkey.monkeyweather.adapter.LifeStyleAdapter
import com.monkey.monkeyweather.adapter.ThreeForecastAdapter
import com.monkey.monkeyweather.api.Api
import com.monkey.monkeyweather.bean.BaseBean
import com.monkey.monkeyweather.bean.NowAirBean
import com.monkey.monkeyweather.bean.SunriseSunsetBean
import com.monkey.monkeyweather.bean.WeatherBean
import com.monkey.monkeyweather.util.LogUtil
import com.monkey.monkeyweather.util.ScreenUtil
import com.monkey.monkeyweather.util.ToastUtil
import com.monkey.monkeyweather.widget.DividerItemDecoration
import com.monkey.monkeyweather.widget.PtrHeader
import kotlinx.android.synthetic.main.fragment_city.*


/**
 * A simple [Fragment] subclass.
 */
class CityFragment : Fragment(), NestedScrollView.OnScrollChangeListener, View.OnClickListener {

    private var mAddress: String = "定位中…"
    private var mCity: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mAddress = arguments!![MainActivity.ADDRESS] as String
        mCity = arguments!![MainActivity.CITY] as String
    }

    fun setLocationData(address: String, city: String) {
        mAddress = address
        mCity = city
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_city, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh_layout.setPtrHandler(OnPullDownToRefreshListener())
        val ptrHeader = PtrHeader(activity!!)
        refresh_layout.headerView = ptrHeader
        refresh_layout.addPtrUIHandler(ptrHeader)
        air_qlty_ll.setOnClickListener(this)
        three_forecast_rv.isNestedScrollingEnabled = false
        three_forecast_rv.layoutManager = LinearLayoutManager(activity)
        three_forecast_rv.addItemDecoration(DividerItemDecoration(activity))
        fifteen_forecast_tv.setOnClickListener(this)
        lifestyle_rv.isFocusable = false
        lifestyle_rv.isNestedScrollingEnabled = false
        lifestyle_rv.layoutManager = LinearLayoutManager(activity)
        lifestyle_rv.addItemDecoration(DividerItemDecoration(activity))

        title_ll.alpha = 0f
        status_view.layoutParams.height = ScreenUtil.getStatusBarHeight(activity!!)
        scroller.setOnScrollChangeListener(this)

        address_tv.text = mAddress
        requestNetwork()

        setBottomLink()
    }

    override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        val titleHeight = resources.getDimensionPixelSize(R.dimen.base_title_height) + ScreenUtil.getStatusBarHeight(activity!!)
        when {//标题栏渐变
            scrollY < wind_ll.top -> title_ll.alpha = 0f//透明
            scrollY in wind_ll.top..(three_forecast_rv.top - titleHeight) -> {//渐变
                val percent = (scrollY - wind_ll.top) * 100 / (three_forecast_rv.top - wind_ll.top - titleHeight)
                title_ll.alpha = percent.toFloat() / 100
            }
            else -> title_ll.alpha = 1f//不透明
        }
        //日出日落控件的动画
        sunrise_sunset_view.startAnim()
    }

    /**
     * 下拉刷新
     */
    private inner class OnPullDownToRefreshListener : PtrDefaultHandler() {
        override fun onRefreshBegin(frame: PtrFrameLayout) {
            address_tv.text = mAddress
            requestNetwork()
        }
    }

    private fun requestNetwork() {
        Api.getWeather(activity!!, mCity, OnWeatherRequestListener())
        Api.getNowAir(activity!!, mCity, OnNowAirRequestListener())
        Api.getSunriseSunset(activity!!, mCity, OnSunriseSunsetRequestListener())
    }

    /**
     * 常规天气集合
     * 3-7天天气预报、实况天气、逐小时预报以及生活指数
     */
    inner class OnWeatherRequestListener : Api.OnNetworkRequestListenerAdapter<BaseBean<List<WeatherBean>>>() {
        override fun onSuccess(result: BaseBean<List<WeatherBean>>) {
            super.onSuccess(result)
            val weather = result.HeWeather6[0]
            if ("ok" == weather.status) {
                val now = weather.now
                title_tv.text = "$mAddress ${now.tmp}℃"
                temp_tv.text = now.tmp
                weather_tv.text = now.cond_txt + " 丨 "
                wind_dir_tv.text = now.wind_dir
                wind_sc_tv.text = now.wind_sc + "级"
                hum_data_tv.text = now.hum + "%"
                fl_data_tv.text = now.fl + "℃"
                pres_data_tv.text = now.pres + "hPa"

                val forecastAdapter = ThreeForecastAdapter(weather.daily_forecast)
                forecastAdapter.onItemClickListener = BaseQuickAdapter.OnItemClickListener(
                        { _: BaseQuickAdapter<Any, BaseViewHolder>, _: View, i: Int ->
                            val intent = Intent(activity, FifteenForecastActivity::class.java)
                            intent.putExtra(MainActivity.CITY, mCity)
                            intent.putExtra(FifteenForecastActivity.SELECT_POSITION, i)
                            startActivity(intent)
                        })
                three_forecast_rv.adapter = forecastAdapter

                val lifeStyleAdapter = LifeStyleAdapter(weather.lifestyle)
                val lifeStyleHeader = View.inflate(activity, R.layout.header_life_style, null)
                lifeStyleAdapter.setHeaderView(lifeStyleHeader)
                lifestyle_rv.adapter = lifeStyleAdapter
            } else {
                ToastUtil.show(activity, weather.status)
            }
        }

        override fun onComplete() {
            super.onComplete()
            refresh_layout.refreshComplete()
        }

        override fun onError(context: Context, e: Throwable) {
            super.onError(context, e)
            refresh_layout.refreshComplete()
        }
    }

    /**
     * 空气质量实况
     */
    inner class OnNowAirRequestListener : Api.OnNetworkRequestListenerAdapter<BaseBean<List<NowAirBean>>>() {
        override fun onSuccess(result: BaseBean<List<NowAirBean>>) {
            super.onSuccess(result)
            val air = result.HeWeather6[0]
            if ("ok" == air.status) {
                val nowCity = air.air_now_city
                air_qlty_tv.text = "空气" + nowCity.qlty
                aqi_tv.text = " " + nowCity.aqi + " "
                aqi_tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.arrow_right, 0)
            } else {
                ToastUtil.show(activity, air.status)
            }
        }

        override fun onComplete() {
            super.onComplete()
            refresh_layout.refreshComplete()
        }
    }

    /**
     * 3-7天日出日落时间
     */
    inner class OnSunriseSunsetRequestListener : Api.OnNetworkRequestListenerAdapter<BaseBean<List<SunriseSunsetBean>>>() {
        override fun onSuccess(result: BaseBean<List<SunriseSunsetBean>>) {
            super.onSuccess(result)
            val weather = result.HeWeather6[0]
            if ("ok" == weather.status) {
                val sunriseSunset = weather.sunrise_sunset[0]
                val date = sunriseSunset.date
                val sr = sunriseSunset.sr
                val ss = sunriseSunset.ss
                sunrise_sunset_view.setSunriseSunsetTime("$date $sr", "$date $ss")
                LogUtil.e("--> $sr $ss")
            } else {
                ToastUtil.show(activity, weather.status)
            }
        }

        override fun onComplete() {
            super.onComplete()
            refresh_layout.refreshComplete()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fifteen_forecast_tv -> {
                val intent = Intent(activity, FifteenForecastActivity::class.java)
                intent.putExtra(MainActivity.CITY, mCity)
                intent.putExtra(FifteenForecastActivity.SELECT_POSITION, 0)
                startActivity(intent)
            }
            R.id.air_qlty_ll -> {
                val intent = Intent(activity, AirQualityActivity::class.java)
                intent.putExtra(MainActivity.ADDRESS, mAddress)
                intent.putExtra(MainActivity.CITY, mCity)
                startActivity(intent)
            }
        }
    }

    /**
     * 底部说明中的csdn、github地址可点击
     */
    private fun setBottomLink() {
        val content = notice_tv.text.toString()
        val ss = SpannableString(content)
        val gitStart = content.indexOf("-") + 1
        val gitEnd = content.lastIndexOf("-")
        val gitLink = content.substring(gitStart, gitEnd)
        val csdnStart = content.indexOf(resources.getString(R.string.nick_name))
        val csdnEnd = csdnStart + resources.getString(R.string.nick_name).length
        val csdnLink = resources.getString(R.string.csdn_blog)
        ss.setSpan(ClickSpan(gitLink), gitStart, gitEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        ss.setSpan(ClickSpan(csdnLink), csdnStart, csdnEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        notice_tv.text = ss
        notice_tv.movementMethod = LinkMovementMethod.getInstance()
    }

    inner class ClickSpan(private var link: String) : ClickableSpan() {

        override fun onClick(widget: View?) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(link)
            startActivity(intent)
        }

        override fun updateDrawState(paint: TextPaint?) {
            paint!!.flags = TextPaint.UNDERLINE_TEXT_FLAG
            paint.color = resources.getColor(R.color.colorPrimaryDark)
        }
    }
}
