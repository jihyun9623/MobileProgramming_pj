package com.hyeontti.drivelikewalking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var fdbAuth:FirebaseAuth
    lateinit var googleSignInClient: GoogleSignInClient
    val RC_SIGN_IN = 99
    lateinit var rdb:DatabaseReference
    var flag = 1

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_studentid,menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    fun init(){
        btn_signup.setOnClickListener {
            startActivity(Intent(this,SignUpActivity::class.java))
        }
        btn_login.setOnClickListener {
            rdb = FirebaseDatabase.getInstance().getReference("DLW/User")
            if (login_id.text.toString() == "") {
                Toast.makeText(applicationContext, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (login_pw.text.toString() == "") {
                Toast.makeText(applicationContext, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }else
                logIn()
        }
        btn_google_login.setOnClickListener {
            signIn()
        }
        nologin.setOnClickListener {
            var userArray = arrayListOf("비회원", "", "", "")
            val i = Intent(applicationContext,MapActivity::class.java)
            i.putExtra("nowUser",userArray)
            startActivity(i)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        fdbAuth = FirebaseAuth.getInstance()
    }

    fun logIn(){
        rdb.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(dbError: DatabaseError) {
                Toast.makeText(applicationContext,"데이터베이스 오류입니다.", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                flag=0
                for (postSnapshot in dataSnapshot.children) {
                    var uid = postSnapshot.getValue(User::class.java)?.uid
                    var upw = postSnapshot.getValue(User::class.java)?.upw
                    var userArray = arrayListOf(uid,upw,
                        postSnapshot.getValue(User::class.java)?.uname,
                        postSnapshot.getValue(User::class.java)?.uemail)
                    Log.d("uidList",uid.toString())
                    if(uid.equals(login_id.text.toString())){
                        flag=1
                        if(upw.equals(login_pw.text.toString())){
                            val i = Intent(applicationContext,MapActivity::class.java)
                            i.putExtra("nowUser",userArray)
                            login_id.text = null
                            login_pw.text = null
                            Toast.makeText(applicationContext,"로그인 성공", Toast.LENGTH_SHORT).show()
                            startActivity(i)
                            break
                        }
                        else {
                            Toast.makeText(applicationContext, "비밀번호가 틀렸습니다.", Toast.LENGTH_SHORT).show()
                            break
                        }
                    }
                }
                if(flag==0)
                    Toast.makeText(applicationContext, "아이디가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            }

        })
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account!=null) {
            Log.d("정보 있냐",account.toString())
            toMainActivity(fdbAuth.currentUser)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try{
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            }catch (e:ApiException){
                Log.w("LoginActivity", "Google sign in failed",e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("LoginActivity","firebaseAuthWithGoogle: "+acct.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken,null)
        fdbAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) {
                task->
                if(task.isSuccessful){
                    Log.w("LoginActivity","firebaseAuthWithGoogle 성공",task.exception)
                    toMainActivity(fdbAuth?.currentUser)
                }else{
                    Log.w("LoginActivity","firebaseAuthWithGoogle 실패",task.exception)
                }
            }
    }

    fun toMainActivity(user:FirebaseUser?){
        if(user !=null){
            Log.d("있냐",user.toString())
            startActivity(Intent(this,MapActivity::class.java))
            finish()
        }
        Log.d("있냐",user.toString())
    }

    fun signIn(){
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}
