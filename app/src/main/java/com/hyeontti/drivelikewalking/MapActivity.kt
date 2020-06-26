package com.hyeontti.drivelikewalking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class MapActivity : AppCompatActivity(), GoogleMap.OnInfoWindowClickListener {
    lateinit var mAuth:FirebaseAuth
    var fusedLocationProviderClient: FusedLocationProviderClient?=null
    var locationCallback: LocationCallback?=null
    var locationRequest : LocationRequest?=null
    lateinit var rdb: DatabaseReference
    val distArray = ArrayList<LatLng>()
    var searchmeter:Double=2000.0
    var searchzoom:Float = 16.0f

    var UInfo = ArrayList<String>()
    var GInfo = ArrayList<String>()

    lateinit var googleMap: GoogleMap
    var loc = LatLng(37.555198, 126.970806)
    var dest:LatLng ?=null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu1 -> {//로그아웃
                signOut()
                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
            R.id.menu2 -> {//내위치로 이동
                dest =null
                getuserlocation()
                startLocationUpdates()
//                googleMap.clear()
            }
            R.id.menu6 -> {//목적지 주변 주차장 찾기
                if(dest !=null)
                    findNearParkinLot(dest!!)
                else
                    Toast.makeText(this,"지도에서 목적지를 선택해주세요.",Toast.LENGTH_SHORT).show()
            }
            R.id.menu4->{
                if(searchmeter>10000)
                    Toast.makeText(this,"현재 검색 범위 : "+searchmeter+"범위는 최대 10KM까지만 검색 가능합니다.",Toast.LENGTH_SHORT).show()
                else{
                    searchmeter+=500.0
                    Toast.makeText(this,"현재 검색 범위 : "+searchmeter.toInt()+"m",Toast.LENGTH_SHORT).show()
                }
            }
            R.id.menu5->{
                if(searchmeter<1000)
                    Toast.makeText(this,"현재 검색 범위 : "+searchmeter+"범위는 최소 1KM까지만 검색 가능합니다.",Toast.LENGTH_SHORT).show()
                else{
                    searchmeter-=500.0
                    Toast.makeText(this,"현재 검색 범위 : "+searchmeter.toInt()+"m",Toast.LENGTH_SHORT).show()
                }
            }
            R.id.menu3 -> {
                dest =null
                findNearParkinLot(loc)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mAuth = FirebaseAuth.getInstance()

        initLocation()
    }

    fun destMapListner(){
        googleMap.setOnMapClickListener {
            googleMap.clear()//마커정보 모두 지워줌
            val options = MarkerOptions()
            options.position(it)
            dest = it
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            googleMap.addMarker(options)
        }
    }

    fun findNearParkinLot(destloc:LatLng){
        rdb = FirebaseDatabase.getInstance().getReference("DLW/Parkinglot/DATA")
        rdb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(dbError: DatabaseError) {
                Toast.makeText(applicationContext,"데이터베이스 오류입니다.", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                distArray.clear()
                googleMap.clear()
                val options = MarkerOptions()
                options.position(destloc)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                googleMap.addMarker(options)
                var parking_name_arr=ArrayList<String>()
                var addr_arr=ArrayList<String>()
                for (postSnapshot in dataSnapshot.children) {
                    var parking_name = postSnapshot.getValue(DATA::class.java)?.parking_name
                    var addr = postSnapshot.getValue(DATA::class.java)?.addr
                    var lat = postSnapshot.getValue(DATA::class.java)?.lat
                    var lng = postSnapshot.getValue(DATA::class.java)?.lng
//                            Log.d("sn?",parking_name.toString())
                    val locationA = Location("point A")
                    locationA.latitude = destloc.latitude
                    locationA.longitude = destloc.longitude
                    val locationB = Location("point B")
                    locationB.latitude = lat!!
                    locationB.longitude = lng!!
                    var onelatlng = LatLng(lat, lng)
                    var distance = locationA.distanceTo(locationB).toDouble()
                    Log.d("distance",distance.toString())
                    if(distance<=searchmeter){//반경 2000미터 안이면
                        distArray.add(onelatlng)
                        parking_name_arr.add(parking_name!!)
                        addr_arr.add(addr!!)
                    }
                }
                Log.d("sndistancesize",(distArray.size-1).toString())
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destloc,14.0f))
                options.title(resources.getString(R.string.myloc))
                googleMap.addMarker(options).showInfoWindow()
                for(i in 0 until distArray.size){
                    val options = MarkerOptions()
                    options.position(distArray[i])
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    options.title(parking_name_arr[i])
                    options.snippet(addr_arr[i])
                    googleMap.addMarker(options)
                }

            }

        })
    }

    override fun onInfoWindowClick(p0: Marker?) {
        UInfo.clear()
        GInfo.clear()
        if(distArray.size==0) return

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.alert_popup,null)

        val parking_name: TextView = view.findViewById(R.id.parking_name)
        val parking_address: TextView = view.findViewById(R.id.parking_address)
        val parking_type: TextView = view.findViewById(R.id.parking_type)
        val parking_type2: TextView = view.findViewById(R.id.parking_type2)
        val parking_call: TextView = view.findViewById(R.id.parking_call)
        val parking_space_all: TextView = view.findViewById(R.id.parking_space_all)
        val parked_car: TextView = view.findViewById(R.id.parked_car)
        val parked_car_time: TextView = view.findViewById(R.id.parked_car_time)
        val parking_free_yn: TextView = view.findViewById(R.id.parking_free_yn)
        val parking_night: TextView = view.findViewById(R.id.parking_night)
        val parking_open_day: TextView = view.findViewById(R.id.parking_open_day)
        val parking_close_day: TextView = view.findViewById(R.id.parking_close_day)
        val parking_open_weekend: TextView = view.findViewById(R.id.parking_open_weekend)
        val parking_close_weekend: TextView = view.findViewById(R.id.parking_close_weekend)
        val parking_basic_pay: TextView = view.findViewById(R.id.parking_basic_pay)
        val parking_basic_time: TextView = view.findViewById(R.id.parking_basic_time)
        val parking_additional_pay: TextView = view.findViewById(R.id.parking_additional_pay)
        val parking_additional_time: TextView = view.findViewById(R.id.parking_additional_time)

        rdb = FirebaseDatabase.getInstance().getReference("DLW/Parkinglot/DATA")
        rdb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(dbError: DatabaseError) {
                Toast.makeText(applicationContext,"데이터베이스 오류입니다.", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (postSnapshot in dataSnapshot.children) {
                    var data = postSnapshot.getValue(DATA::class.java)
                    if(data?.parking_name==p0!!.title){
                        parking_name.text = data?.parking_name
                        parking_address.text = data?.addr
                        parking_type.text = data?.parking_type_nm
                        parking_type2.text = data?.operation_rule_nm
                        parking_call.text = data?.tel
                        parking_space_all.text = data?.capacity.toString()
                        if(data?.cur_parking==""){
                            parked_car.text = "미연계중"
                                //postSnapshot.getValue(DATA::class.java)?.cur_parking
                            parked_car_time.text = "미연계중"
                                //postSnapshot.getValue(DATA::class.java)?.cur_parking_time
                        }
                        parking_free_yn.text = data?.pay_nm
                        parking_night.text = data?.night_free_open_nm
                        parking_open_day.text = data?.weekday_begin_time
                        parking_close_day.text = data?.weekday_end_time
                        parking_open_weekend.text = data?.weekend_begin_time
                        parking_close_weekend.text = data?.weekend_end_time
                        parking_basic_pay.text = data?.rates.toString()
                        parking_basic_time.text = data?.time_rate.toString()
                        parking_additional_pay.text = data?.add_rates.toString()
                        parking_additional_time.text = data?.add_time_rate.toString()
                        Log.d("parkinglot",parking_name.text.toString())
                        break
                    }
                }
            }
        })

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.parking_detail)
            .setPositiveButton(R.string.call){ _,_->
                Log.d("call","전화 걸기")
                if(parking_call.text!="")
                    startActivity(Intent("android.intent.action.DIAL", Uri.parse("tel:"+parking_call.text)))
                else
                    Toast.makeText(this,"전화번호가 없습니다.",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.write_review){ _, _->
                val i = Intent(this,ReviewActivity::class.java)
                if (mAuth.currentUser != null) {//구글 로그인이면
                    GInfo.add(mAuth.currentUser?.getDisplayName()!!)
                    GInfo.add(mAuth.currentUser?.getEmail()!!)
                    GInfo.add(p0!!.title.toString())
                    i.putExtra("nowData",GInfo)
                    Log.i("nowUser",GInfo.toString())
                    startActivity(i)
                }else{
                    UInfo = intent?.getStringArrayListExtra("nowUser")!!
                    UInfo.add(p0!!.title.toString())
                    i.putExtra("nowData",UInfo)
                    Log.i("nowUser", UInfo.toString())
                    if(UInfo[0]!="비회원")
                        startActivity(i)
                }
            }
            .setNeutralButton(R.string.check,null)
            .create()

        alertDialog.setView(view)
        alertDialog.show()
    }

    fun initLocation(){
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            getuserlocation()
            startLocationUpdates()
            initmap()
        }else{
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),100)
        }
    }

    fun getuserlocation(){
        fusedLocationProviderClient = LocationServices
            .getFusedLocationProviderClient(this)
        Log.i("MyLoc-getuserLocation",loc.latitude.toString()+", "+loc.longitude.toString())
        fusedLocationProviderClient?.lastLocation?.addOnSuccessListener {
            try {
                loc = LatLng(it.latitude, it.longitude)
                Log.i(
                    "MyLoc-getuserLocation",
                    it.latitude.toString() + ", " + it.longitude.toString()
                )
//                googleMap.clear()
                val options = MarkerOptions()
                options.position(loc)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                options.title(resources.getString(R.string.myloc))
//                options.snippet("건국대학교")
                val mk1 = googleMap.addMarker(options)
                mk1.showInfoWindow()
            }catch (e:IllegalStateException){
                Toast.makeText(this,"위치를 켜주세요!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED&&
                grantResults[1]==PackageManager.PERMISSION_GRANTED){
                getuserlocation()
                startLocationUpdates()
                initmap()
            }else{
                Toast.makeText(this,"위치정보 제공을 하셔야 합니다.",Toast.LENGTH_SHORT).show()
                initmap()
            }
        }
    }

    fun startLocationUpdates(){
        locationRequest = LocationRequest.create()?.apply {
            interval = 600000
            fastestInterval = 50000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        Log.i("MyLoc-startLocationUpdates",loc.latitude.toString()+", "+loc.longitude.toString())
        locationCallback = object  : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for(location in locationResult.locations){
                    loc = LatLng(location.latitude,location.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,16.0f))
                }
            }
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    fun stopLocationUpdates(){
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    fun initmap(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync{//lambda
            googleMap = it
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,16.0f))
            googleMap.setMinZoomPreference(10.0f)
            googleMap.setMaxZoomPreference(18.0f)
            Log.i("MyLoc-initmap",loc.latitude.toString()+", "+loc.longitude.toString())
            val options = MarkerOptions()
            options.position(loc)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//            options.title("역")
//            options.snippet("서울역")
            googleMap.addMarker(options)
            googleMap.setOnInfoWindowClickListener(this)

            destMapListner()
        }
    }



    private fun signOut() {FirebaseAuth.getInstance().signOut()
        FirebaseAuth.getInstance().signOut()
    }
//    private fun revokeAccess() {
//        mAuth.currentUser!!.delete()
//    }


}
