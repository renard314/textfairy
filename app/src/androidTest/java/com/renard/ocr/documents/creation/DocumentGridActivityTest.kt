package com.renard.ocr.documents.creation

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.RawRes
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.renard.ocr.R
import com.renard.ocr.TextFairyApplication
import com.renard.ocr.documents.viewing.grid.DocumentGridActivity
import com.renard.ocr.test.R.raw.*
import com.renard.ocr.util.PreferencesUtils
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class DocumentGridActivityTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(DocumentGridActivity::class.java)


    @Before
    fun setup() {
        Intents.init()
        val textFairyApplication: TextFairyApplication = getApplicationContext()
        IdlingRegistry.getInstance().register(textFairyApplication.espressoTestIdlingResource)

        savePickedImage(getApplicationContext(), multiple_columns)
        savePickedImage(getApplicationContext(), one_column_test_image)
        savePickedImage(getApplicationContext(), blurred)
        PreferencesUtils.setFirstScan(textFairyApplication, false)
        PreferencesUtils.setNumberOfSuccessfulScans(textFairyApplication, 0)
    }

    @After
    fun after() {
        Intents.release()
        val textFairyApplication: TextFairyApplication = getApplicationContext()
        IdlingRegistry.getInstance().unregister(textFairyApplication.espressoTestIdlingResource)
    }

    @Test
    fun simple_layout_gallery() {
        openGallery(one_column_test_image)
        pressCropFinished()
        selectSimpleLayout()
        pressDialogOk()
        checkDocumentActivityOpened()
    }

    @Test
    fun simple_layout_gallery_press_back_in_crop() {
        openGallery(one_column_test_image)
        pressBackButton()
        checkDocumentGridIsShown()
    }

    @Test
    fun simple_layout_gallery_press_back_in_layout_selection() {
        openGallery(one_column_test_image)
        pressCropFinished()
        pressBackButton()
        checkDocumentGridIsShown()
    }

    @Test
    fun simple_layout_gallery_press_cancel_in_layout_selection() {
        openGallery(one_column_test_image)
        pressCropFinished()
        pressDialogButton2()
        checkDocumentGridIsShown()
    }


    @Test
    fun complex_layout_gallery_press_back_in_textblock_selection() {
        openGallery(multiple_columns)
        pressCropFinished()
        selectColumnLayout()
        pressDialogOk()
        pressBackButton()
        checkDocumentGridIsShown()
    }

    @Test
    fun blurred_image_gallery_new_image() {
        openGallery(blurred)
        onView(withId(R.id.blur_warning_title)).check(matches(isDisplayed()))
        galleryReturns(one_column_test_image)
        pressDialogOk()
        pressCropFinished()
        selectSimpleLayout()
        pressDialogOk()
        checkDocumentActivityOpened()
    }

    @Test
    fun blurred_image_gallery_continue() {
        openGallery(blurred)
        onView(withId(R.id.blur_warning_title)).check(matches(isDisplayed()))
        pressDialogButton2()
        pressBackButton()
        checkDocumentGridIsShown()
    }

    @Test
    fun complex_layout_gallery() {
        openGallery(multiple_columns)
        pressCropFinished()
        selectColumnLayout()
        pressDialogOk()
        selectColumn()
        pressColumnSelectionFinished()
        checkDocumentActivityOpened()
    }

    private fun createImageGallerySetResultStub(@RawRes testImageId: Int): Instrumentation.ActivityResult {
        val context = getApplicationContext<Context>()
        val resultData = Intent()
        resultData.data = File(context.cacheDir, "$testImageId.jpeg").toUri()
        return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
    }

    private fun galleryReturns(@RawRes testImageId: Int) {
        intending(hasAction(Intent.ACTION_CHOOSER))
                .respondWith(createImageGallerySetResultStub(testImageId))
    }

    private fun savePickedImage(context: Context, @RawRes testImageId: Int) {
        val resources = InstrumentationRegistry.getInstrumentation().context.resources
        val bm = BitmapFactory.decodeResource(resources, testImageId)
        val file = File(context.cacheDir, "$testImageId.jpeg")
        if (file.exists()) {
            file.delete()
        }
        FileOutputStream(file).use {
            bm.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
        }
    }

    private fun pressDialogButton2() {
        onView(withId(android.R.id.button2)).perform(click())
    }

    private fun pressDialogOk() {
        onView(withId(android.R.id.button1)).perform(click())
    }

    private fun selectColumnLayout() {
        onView(withId(R.id.column_layout)).perform(click())
    }

    private fun pressCropFinished() {
        onView(withId(R.id.item_save)).perform(click())
    }

    private fun openGallery(@RawRes testImageId: Int) {
        galleryReturns(testImageId)
        onView(withId(R.id.item_gallery)).perform(click())
    }

    private fun checkDocumentGridIsShown() {
        onView(withId(R.id.gridview)).check(matches(isDisplayed()))
    }

    private fun pressBackButton() {
        onView(isRoot()).perform(ViewActions.pressBack())
    }

    private fun selectColumn() {
        onView(withId(R.id.progress_image)).perform(ViewActions.swipeLeft())
    }

    private fun checkDocumentActivityOpened() {
        intending(hasComponent(DocumentGridActivity::class.qualifiedName))
    }

    private fun selectSimpleLayout() {
        onView(withId(R.id.page_layout)).perform(click())
    }

    private fun pressColumnSelectionFinished() {
        onView(withId(R.id.column_pick_completed)).perform(click())
    }

}

