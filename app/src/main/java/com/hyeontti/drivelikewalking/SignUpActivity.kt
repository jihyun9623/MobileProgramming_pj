package com.hyeontti.drivelikewalking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_sign_up.*


class SignUpActivity : AppCompatActivity() {
    lateinit var rdb: DatabaseReference
    var flag = 1

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_studentid,menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        init()
    }

    fun init(){
        rdb = FirebaseDatabase.getInstance().getReference("DLW/User")

        signup_confirm.setOnClickListener {
            if(signup_id.text.toString() != "") {
                rdb.addListenerForSingleValueEvent(object : ValueEventListener {//아이디 중복검사
                    override fun onCancelled(dbError: DatabaseError) {
                        Toast.makeText(applicationContext,"데이터베이스 오류입니다.",Toast.LENGTH_SHORT).show()
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (postSnapshot in snapshot.children) {
                            var uid = postSnapshot.getValue(User::class.java)?.uid
                            Log.d("uidList",uid.toString())
                            if(uid.equals(signup_id.text.toString())) {
                                flag = 0
                                Toast.makeText(applicationContext, "아이디 중복입니다.", Toast.LENGTH_SHORT).show()
                                if(flag==0)
                                    return
                            }else{
                                flag=1
                            }
                        }
                        if(flag ==1){//중복이 아님
                            if (signup_pw.text.toString() == "") {
                                Toast.makeText(applicationContext, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                            } else if (signup_pw_check.text.toString() == "") {
                                Toast.makeText(applicationContext, "비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show()
                            } else if (signup_pw.text.toString() != signup_pw_check.text.toString()) {
                                Toast.makeText(applicationContext, "비밀번호가 다릅니다.", Toast.LENGTH_SHORT).show()
                            } else
                                registerUser()
                        }
                    }
                })
            } else
                Toast.makeText(applicationContext, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun registerUser(){
        val user:User
        if(signup_email==null||signup_name==null){
            user = User(
                "-", signup_id.text.toString(),
                signup_pw.text.toString(), "-"
            )
        }else {
            user = User(
                signup_name.text.toString(), signup_id.text.toString(),
                signup_pw.text.toString(), signup_email.text.toString()
            )
        }
        rdb.child(signup_id.text.toString()).setValue(user)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
