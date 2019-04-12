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

package com.example.android.trackmysleepquality.sleepquality

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import kotlinx.coroutines.*

const val LOG_TAG = "SleepQualityViewModel"

// COMPLETED (03) Using the code in SleepTrackerViewModel for reference, create SleepQualityViewModel
// with coroutine setup and navigation setup.
class SleepQualityViewModel(
        private val sleepNightKey: Long = 0L,
        val database: SleepDatabaseDao) : ViewModel() {

    private val viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    /** Encapsulated event variable for the 'should-navigate-back-to-sleep-tracker-fragment' event */
    private val _navigateToSleepTracker = MutableLiveData<Boolean>(false)

    val navigateToSleepTracker: LiveData<Boolean>
        get() = _navigateToSleepTracker

    fun doneNavigating() {
        _navigateToSleepTracker.value = false
    }

    // COMPLETED (04) implement the onSetSleepQuality() click handler using coroutines.
    /**
     * OnClickHandler for the sleep quality ImageViews
     */
    fun onSetSleepQuality(quality: Int) {
        uiScope.launch {
            // run the db operations in another scope: the IO-scope
            withContext(Dispatchers.IO) {
                // Get the SleepNight for the ID that has been passed as fragment args from the db.
                val tonight = database.get(sleepNightKey) ?: return@withContext
                // Set its sleep quality property ...
                tonight.sleepQuality = quality
                // ...and update the corresponding sleep night record in the db.
                database.update(tonight)
            }
            // We've initiated the db update, so we can now navigate back to the sleep tracker fragment
            _navigateToSleepTracker.value = true
        }
    } // close fun onSetSleepQuality()

    /**
     * We'll cancel all still running coroutines when this ViewModel gets destroyed
     */
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }


} // close class SleepQualityViewModel