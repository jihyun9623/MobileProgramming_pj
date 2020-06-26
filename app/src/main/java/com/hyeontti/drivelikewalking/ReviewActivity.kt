package com.hyeontti.drivelikewalking

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_review.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class ReviewActivity : AppCompatActivity() {
    lateinit var layoutManager: LinearLayoutManager
    lateinit var adapter: MyReviewAdapter
    lateinit var rdb: DatabaseReference
    var findQuery = false

    var Info = ArrayList<String>()

    private var translate: Translate? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_studentid,menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        Info = intent?.getStringArrayListExtra("nowData")!!
        Log.i("nowData", Info.toString())
        init()
        initBtn()
    }

    fun initBtn(){
        //insert
        done_btn.setOnClickListener {
            val item = Reviews(
                Info[0],
                ratingBar.rating,
                et_contents.text.toString()
            )
            initAdapter()
            rdb.child(Info[0]).setValue(item)
            ratingBar.rating = 0.0f
            et_contents.setText("")
        }
        sort_star.setOnClickListener {
            if(findQuery)
                findQueryAdapter()
            else{
                findQuery=true
                findQueryAdapter()
            }
        }
    }

    fun initAdapter(){
        if(findQuery){
            findQuery = false
            if(adapter!=null)
                adapter.stopListening()
            layoutManager.reverseLayout = false
            layoutManager.stackFromEnd = false
            val query = FirebaseDatabase.getInstance().reference
                .child("DLW/Reviews/"+Info[Info.size-1]).limitToLast(50)
            val option = FirebaseRecyclerOptions.Builder<Reviews>()
                .setQuery(query,Reviews::class.java).build()
            adapter = MyReviewAdapter(option)
            recyclerView.adapter = adapter
            translate()
            adapter.startListening()
        }
    }

    fun findQueryAdapter(){
        if(adapter!=null)//어댑터 새로만들기
            adapter.stopListening()
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager
        val query = FirebaseDatabase.getInstance().reference
            .child("DLW/Reviews/"+Info[Info.size-1]).orderByChild("star").limitToLast(50)
        val option = FirebaseRecyclerOptions.Builder<Reviews>()
            .setQuery(query,Reviews::class.java).build()
        adapter = MyReviewAdapter(option)
        recyclerView.adapter = adapter
        translate()
        adapter.startListening()
    }

    fun init(){
        pl_name.text = Info[Info.size-1]
        layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        recyclerView.layoutManager = layoutManager
        rdb = FirebaseDatabase.getInstance().getReference("DLW/Reviews").child(Info[Info.size-1])
        val query = FirebaseDatabase.getInstance().reference
            .child("DLW/Reviews/"+Info[Info.size-1]).limitToLast(50)
        val option = FirebaseRecyclerOptions.Builder<Reviews>()
            .setQuery(query,Reviews::class.java).build()
        adapter = MyReviewAdapter(option)
        translate()
        recyclerView.adapter = adapter
    }

    fun translate(){
        adapter.itemClickListner = object :MyReviewAdapter.OnItemClickListner{
            override fun OnItemClick(view: View, position: Int) {
                getTranslateService()
                if(Locale.getDefault().language=="ko"){
                    val originalText = adapter.getItem(position).contents
                    val translation = translate!!.translate(originalText, Translate.TranslateOption.targetLanguage("ko"), Translate.TranslateOption.model("base"))
                    Toast.makeText(applicationContext,translation.translatedText,Toast.LENGTH_SHORT).show()
                }else if(Locale.getDefault().language=="en"){
                    val originalText = adapter.getItem(position).contents
                    val translation = translate!!.translate(originalText, Translate.TranslateOption.targetLanguage("en"), Translate.TranslateOption.model("base"))
                    Toast.makeText(applicationContext,translation.translatedText,Toast.LENGTH_SHORT).show()
                }

            }

        }
    }

    private fun getTranslateService() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        try {
            resources.openRawResource(R.raw.googletranscredential).use { `is` ->
                val myCredentials = GoogleCredentials.fromStream(`is`)
                val translateOptions = TranslateOptions.newBuilder().setCredentials(myCredentials).build()
                translate = translateOptions.service
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()//데이터베이스가 변경되었으면 말해줘
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()//리스닝 그만해
    }
}
