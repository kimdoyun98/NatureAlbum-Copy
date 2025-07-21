package com.and04.naturealbum.background.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import com.and04.naturealbum.data.dto.FirebaseLabel
import com.and04.naturealbum.data.dto.FirebasePhotoInfo
import com.and04.naturealbum.data.localdata.room.HazardAnalyzeStatus
import com.and04.naturealbum.data.localdata.room.PhotoDetailDao
import com.and04.naturealbum.data.repository.RetrofitRepository
import com.and04.naturealbum.data.repository.firebase.AlbumRepository
import com.and04.naturealbum.utils.image.ImageConvert
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseInsertService : Service() {
    @Inject
    lateinit var albumRepository: AlbumRepository

    @Inject
    lateinit var retrofitRepository: RetrofitRepository

    @Inject
    lateinit var photoDetailDao: PhotoDetailDao
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val uid = Firebase.auth.currentUser!!.uid
            val insertPhoto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(FIREBASE_INSERT_DATA, InsertPhoto::class.java)!!
            } else {
                intent?.getParcelableExtra<InsertPhoto>(FIREBASE_INSERT_DATA)!!
            }

            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Log.e("FirebaseInsertService-onStartCommand", "$throwable : 이미지 저장 오류")
                stopService(intent)
            }

            job = scope.launch(exceptionHandler) {
                if (!isCleanImage(insertPhoto.uri, insertPhoto.fileName)) {
                    stopService(intent)
                    return@launch
                }

                val storageUriDeferred = async {
                    albumRepository.saveImageFile(
                        uid = uid,
                        label = insertPhoto.label.name,
                        fileName = insertPhoto.fileName,
                        uri = insertPhoto.uri.toUri()
                    )
                }
                val serverNoLabelDeferred = async {
                    val serverLabels = albumRepository.getLabelsToList(uid).getOrThrow()
                    serverLabels.none { serverLabel ->
                        serverLabel.labelName == insertPhoto.label.name
                    }
                }

                val storageUri = storageUriDeferred.await()
                val serverNoLabel = serverNoLabelDeferred.await()

                val insertLabelJob = launch {
                    if (serverNoLabel) {
                        albumRepository
                            .insertLabel(
                                uid = uid,
                                labelName = insertPhoto.label.name,
                                labelData = FirebaseLabel(
                                    backgroundColor = insertPhoto.label.backgroundColor,
                                    thumbnailUri = storageUri.toString(),
                                    fileName = insertPhoto.fileName
                                )
                            )
                    }
                }

                val insertPhotoInfoJob = launch {
                    albumRepository.insertPhotoInfo(
                        uid = uid,
                        fileName = insertPhoto.fileName,
                        photoData = FirebasePhotoInfo(
                            uri = storageUri.toString(),
                            label = insertPhoto.label.name,
                            latitude = insertPhoto.latitude,
                            longitude = insertPhoto.longitude,
                            description = insertPhoto.description,
                            datetime = insertPhoto.dateTime
                        )
                    )
                }

                joinAll(insertLabelJob, insertPhotoInfoJob)
            }

        } catch (e: NullPointerException) {
            Log.e("FirebaseInsertService", e.toString())
        } finally {
            stopService(intent)
        }

        return START_NOT_STICKY
    }

    private suspend fun isCleanImage(
        uri: String,
        fileName: String
    ): Boolean {
        val imgEncoding = ImageConvert.getBase64FromUri(applicationContext, uri)

        val hazardMapperResult =
            retrofitRepository.analyzeHazardWithGreenEye(imgEncoding)
        if (hazardMapperResult == HazardAnalyzeStatus.FAIL) {
            photoDetailDao.updateHazardCheckResultByFIleName(
                HazardAnalyzeStatus.FAIL,
                fileName
            )

            return false
        } else {
            photoDetailDao.updateHazardCheckResultByFIleName(
                HazardAnalyzeStatus.PASS,
                fileName
            )
        }

        return true
    }

    override fun onBind(p0: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        const val FIREBASE_INSERT_DATA = "firebase_insert_data"
        const val SERVICE_LABEL = "service_label"
    }
}
