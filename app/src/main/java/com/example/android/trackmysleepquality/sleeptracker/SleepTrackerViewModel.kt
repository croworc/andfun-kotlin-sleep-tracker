/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    //COMPLETED (01) Declare Job() and cancel jobs in onCleared().
    private var viewModelJob = Job()


    // COMPLETED (02) Define uiScope for coroutines. The scope determines which thread the coroutine
    // will run in, and it also needs to know about the job.
    // The coroutines launched in the UI scope will  run on the UI thread.
    // This is sensible for the view model, as the coroutines will eventually update the UI.
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // COMPLETED (03) Create a MutableLiveData variable 'tonight' for one SleepNight.
    private var tonight = MutableLiveData<SleepNight?>()

    // COMPLETED (04) Define a variable, nights. Then getAllNights() from the database
    // and assign to the nights variable. Its a LiveData<List<SleepNight>>.
    private val nights = database.getAllNights()

    // COMPLETED (12) Transform nights into a nightsString using formatNights().
    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    // COMPLETED (05) In an init block, initializeTonight(), and implement it to launch a coroutine
    //to getTonightFromDatabase().
    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        // Launch a coroutine in the current scope without blocking the current thread
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    // COMPLETED (06) Implement getTonightFromDatabase()as a suspend function.
    // This method will be called from inside the coroutine that runs in the UI scope
    // and must not block.
    // We want to return a SleepNight, or null.
    private suspend fun getTonightFromDatabase(): SleepNight? {
        return withContext(Dispatchers.IO) {
            var night = database.getTonight() // this returns the latest night saved in the db
            // Is this latest sleep recording already completed (start- and end time differ)?
            // Then it's not a started recording (for 'tonight') and we will return 'null'.
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    // COMPLETED (07) Implement the click handler for the Start button, onStartTracking(), using
    // coroutines. Define the suspend function insert(), to insert a new night into the database.
    fun onStartTracking() {
        // We do this in the UI scope, as we finally need to update the UI w/ the new data.
        uiScope.launch {
            // Create a new sleep recording object for this night; it captures the current time as
            // the start time.
            val newNight = SleepNight()
            // Insert it into the db
            insert(newNight)
            // Retrieve that new sleep recording object that we've just inserted back from the db,
            // because SQLite has provided the ID on its own.
            tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        // Again, the long running work (here: inserting into the db) has nothing to do w/ the UI,
        // so we switch the context to the IO scope, that is optimized for these kind of operations.
        withContext(Dispatchers.IO) {
            database.insert(night) // calling the DAO function insert()
        }
    }

    // COMPLETED (08) Create onStopTracking() for the Stop button with an update() suspend function.
    fun onStopTracking() {
        uiScope.launch {
            // 'tonight' is null if either:
            //    a) we've just started the app for the first time, or
            //    b) there's currently no sleep record in the db, or
            //    c) the latest/only sleep record is not 'open', i.e. the end time is different from
            //       the start time.
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
        }
    }

    private suspend fun update(night: SleepNight) {
        // Again, we're running the db update operation in a coroutine that runs in a different
        // scope from the UI scope.
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }

    //COMPLETEDk (09) For the Clear button, created onClear() with a clear() suspend function.
    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel() // cancels all coroutines
    }

} // close class SleepTrackerViewModel

