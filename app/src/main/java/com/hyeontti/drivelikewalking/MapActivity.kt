package com.hyeontti.drivelikewalking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class MapActivity : AppCompatActivity() {
    lateinit var mAuth:FirebaseAuth
    var fusedLocationProviderClient: FusedLocationProviderClient?=null
    var locationCallback: LocationCallback?=null
    var locationRequest : LocationRequest?=null
    lateinit var rdb: DatabaseReference
    val distArray = ArrayList<LatLng>()
    var searchmeter:Double=2000.0
    var searchzoom:Float = 16.0f
    var searchpnum = ArrayList<Int>()

    lateinit var googleMap: GoogleMap
    var loc = LatLng(37.555198, 126.970806)
    val arrLoc = ArrayList<LatLng>()

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
                getuserlocation()
                startLocationUpdates()
                googleMap.clear()
            }
            R.id.menu4->{
                if(searchmeter>10000)
                    Toast.makeText(applicationContext,"현재 검색 범위 : "+searchmeter+"범위는 최대 10KM까지만 검색 가능합니다.",Toast.LENGTH_SHORT).show()
                else{
                    searchmeter+=500.0
                    Toast.makeText(applicationContext,"현재 검색 범위 : "+searchmeter.toInt()+"m",Toast.LENGTH_SHORT).show()
                }
            }
            R.id.menu5->{
                if(searchmeter<1000)
                    Toast.makeText(applicationContext,"현재 검색 범위 : "+searchmeter+"범위는 최소 1KM까지만 검색 가능합니다.",Toast.LENGTH_SHORT).show()
                else{
                    searchmeter-=500.0
                    Toast.makeText(applicationContext,"현재 검색 범위 : "+searchmeter.toInt()+"m",Toast.LENGTH_SHORT).show()
                }
            }
            R.id.menu3 -> {
                rdb = FirebaseDatabase.getInstance().getReference("DLW/Parkinglot/DATA")
                rdb.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(dbError: DatabaseError) {
                        Toast.makeText(applicationContext,"데이터베이스 오류입니다.", Toast.LENGTH_SHORT).show()
                    }
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        distArray.clear()
                        searchpnum.clear()
                        googleMap.clear()
                        val options = MarkerOptions()
                        options.position(loc)
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                        googleMap.addMarker(options)
                        var parking_name=ArrayList<String>()
                        var addr=ArrayList<String>()
                        for (postSnapshot in dataSnapshot.children) {
                            searchpnum.add(postSnapshot.key!!.toInt())
//                            var a = postSnapshot.getValue(ParkingLot::class.java)!!
//                            Log.d("sn?",a.lat.toString())
                            var onelat=0.0
                            var onelng=0.0
                            var distance = 0.0
                            var flag = 0
//                            var nowData = postSnapshot.children
//                            Log.d("nowData",nowData.toString())
                            for(ss in postSnapshot.children){
                                if(flag==1) break
                                var temp1=""
                                var temp2=""
//                                Toast.makeText(applicationContext,"위도경도",Toast.LENGTH_SHORT).show()
                                if(ss.key.equals("parking_name")){
                                    Log.d("sn",ss.toString())
                                    temp1 = ss.value.toString()
                                }
                                else if(ss.key.equals("addr")){
                                    Log.d("sn",ss.toString())
                                    temp2 = ss.value.toString()
                                }
                                else if(ss.key.equals("lat")){
                                    Log.d("sn",ss.toString())
                                    onelat = ss.value.toString().toDouble()
                                }
                                else if(ss.key.equals("lng")){
                                    Log.d("sn",ss.toString())
                                    onelng = ss.value.toString().toDouble()
                                }
                                if(onelat!=0.0&&onelng!=0.0){
                                    val locationA = Location("point A")
                                    locationA.latitude = loc.latitude
                                    locationA.longitude = loc.longitude
                                    val locationB = Location("point B")
                                    locationB.latitude = onelat
                                    locationB.longitude = onelng
                                    var onelatlng = LatLng(onelat, onelng)
                                    distance = locationA.distanceTo(locationB).toDouble()
                                    Log.d("sndistance",distance.toString())
                                    if(distance<=searchmeter){//반경 2000미터 안이면
                                        distArray.add(onelatlng)
                                        parking_name.add(temp1)
                                        addr.add(temp2)
                                        flag=1
                                    }
                                }
                            }
//                            if(distArray.size>2)
//                                break
                        }
                        Log.d("sndistancesize",(distArray.size-1).toString())
                        for(i in 0 until distArray.size){
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc,14.0f))
                            val options = MarkerOptions()
                            options.position(distArray[i])
                            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            options.title(parking_name[i])
                            options.snippet(addr[i])
                            googleMap.addMarker(options)
//                            mk1.showInfoWindow()
                        }

                    }

                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mAuth = FirebaseAuth.getInstance()

        var UID = intent?.getStringArrayListExtra("nowUser").toString()
        Log.i("nowUser",UID)
        initLocation()
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
                googleMap.clear()
                val options = MarkerOptions()
                options.position(loc)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                options.title("현재 위치")
                options.snippet("건국대학교")
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
            val mk1 = googleMap.addMarker(options)
//            mk1.showInfoWindow()

            //클릭한곳의 위도/경도 가져오기
            initMapListner()
        }
    }

    fun initMapListner(){
        googleMap.setOnMapClickListener {
            googleMap.clear()//마커정보 모두 지워줌
            arrLoc.add(it)
            val options = MarkerOptions()
            options.position(it)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            googleMap.addMarker(options)
            //폴리라인
//            val lineOptions = PolylineOptions().color(Color.GREEN).addAll(arrLoc)
//            googleMap.addPolyline(lineOptions)
            //폴리곤
//            val option2 = PolygonOptions()
//                .fillColor(Color.argb(100,255,255,0))
//                .strokeColor(Color.GREEN).addAll(arrLoc)
//            googleMap.addPolygon(option2)

        }
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
    private fun revokeAccess() {
        mAuth.currentUser!!.delete()
    }


}