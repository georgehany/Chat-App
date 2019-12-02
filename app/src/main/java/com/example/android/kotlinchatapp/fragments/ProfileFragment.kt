package com.example.android.kotlinchatapp.fragments


import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import com.bumptech.glide.Glide
import com.example.android.kotlinchatapp.Model.User
import com.example.android.kotlinchatapp.R
import com.example.android.kotlinchatapp.profile_photo.ProfilePhotoActivity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import de.hdodenhof.circleimageview.CircleImageView
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import kotlinx.android.synthetic.main.fragment_profile.view.profile_image
import kotlinx.android.synthetic.main.fragment_profile.view.user_name
import java.io.IOException
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */

class ProfileFragment : Fragment() {
    private val PICK_IMAGE_REQUEST = 71
    private var filePath: Uri? = null
    private var firebaseStore: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    lateinit var firebaseUser: FirebaseUser
    lateinit var reference: DatabaseReference
    lateinit var user: User
    lateinit var progressDialog:ProgressDialog
    lateinit var profile:CircleImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v= inflater.inflate(R.layout.fragment_profile, container, false)

        firebaseStore = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        firebaseUser= FirebaseAuth.getInstance().currentUser!!
//        Toast.makeText(this,firebaseUser?.displayName,Toast.LENGTH_LONG).show()
//        user_name.text=firebaseUser?.displayName

        reference= FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.uid)
        Log.e("idd",firebaseUser.uid)




        return v
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        progressDialog=ProgressDialog(context)
        progressDialog.setMessage("Uploading")
        progressDialog.setCancelable(false)
        v.profile_image.setOnClickListener {
            val intent=Intent(context,ProfilePhotoActivity::class.java)
            intent.putExtra("profilePhoto",user.imageURL)
            val option =ActivityOptionsCompat.makeSceneTransitionAnimation(activity!!,v.profile_image,getString(R.string.profile_photo))
            startActivity(intent,option.toBundle())
        }
        v.edit_profile_image.setOnClickListener { launchGallery() }
        v.edit_user_name.setOnClickListener{
            v.user_name.visibility= INVISIBLE
            v.user_name_edit.visibility= VISIBLE
            v.save_edit_user_name.visibility= VISIBLE
            v.edit_user_name.visibility= GONE
        }
        v.save_edit_user_name.setOnClickListener {
            editUserName(v)

            progressDialog.show()
        }
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                user=dataSnapshot.getValue(User::class.java)!!
                v.user_name.text=user?.userName
                v.user_name_edit.setText( user?.userName)
                if(user!!.imageURL=="default")
                    v.profile_image.setImageResource(R.mipmap.ic_launcher_round)
                else {
                    Glide.with(context!!).load(user!!.imageURL).into(v.profile_image).waitForLayout()
                }
            }

        })

    }
    private fun editUserName(v:View) {
        val newName=user_name_edit.text.toString()
        reference.child("userName").setValue(newName )
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful)
                {
                    Toasty.success(context!!, "saved", Toasty.LENGTH_LONG).show()
                    v.user_name.visibility = VISIBLE
                    v.user_name_edit.visibility = GONE
                    v.save_edit_user_name.visibility = GONE
                    v.edit_user_name.visibility = VISIBLE
                    progressDialog.dismiss()
            }
            }).addOnFailureListener {
                Toasty.error(context!!,it.message.toString(),Toasty.LENGTH_LONG).show()
                progressDialog.hide()
            }
        reference.child("search").setValue(v.user_name_edit.text.toString().toLowerCase())


    }




    private fun launchGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_IMAGE_REQUEST&&resultCode== Activity.RESULT_OK){
            if (data==null||data.data==null)
                return
            filePath=data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, filePath)
                profile_image.setImageBitmap(bitmap)

                uploadImage()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.show()

        if (filePath!=null){
            val ref =storageReference?.child("  uploads/"+ UUID.randomUUID().toString())
            val uploadTask=ref?.putFile(filePath!!)

            val urlTask=uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task->
                if (!task.isSuccessful){
                    task.exception?.let {
                        throw it
                        progressDialog.hide()
                    }
                }
                return@Continuation ref.downloadUrl

            })?.addOnCompleteListener{
                if (it.isSuccessful){
                    val downloadUri=it.result
                    updateProfile(downloadUri.toString())

                }
                else{
                    Toasty.error(context!!,"failed to upload", Toasty.LENGTH_LONG).show()
                    progressDialog.hide()
                }
            }?.addOnFailureListener {
                Toasty.error(context!!,it.message.toString(), Toasty.LENGTH_LONG).show()
                progressDialog.hide()
            }
        }
        else{
            Toast.makeText(context!!,"file path = null", Toast.LENGTH_LONG).show()
            progressDialog.hide()
        }
    }
    private fun updateProfile(path: String) {
        user?.imageURL=path.toString()
        Log.e("userim",user.imageURL.toString())
        val hashMap=HashMap<String,String>()
        hashMap?.put("id",user.id)
        hashMap?.put("userName",user.userName)

        hashMap?.put("imageURL",path)
        //val hash=user.to
        reference.child("imageURL").setValue(path )
            .addOnCompleteListener(OnCompleteListener {
                if (it.isSuccessful){
                    Toasty.success(context!!, "saved", Toasty.LENGTH_LONG).show()
                    progressDialog.hide()
            }
            }).addOnFailureListener {
                Toasty.error(context!!,it.message.toString(),Toasty.LENGTH_LONG).show()
                progressDialog.hide()
            }
    }


}
