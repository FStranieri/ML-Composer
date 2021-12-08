package com.fs.sample.cameraxMLsample.UI

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.fs.sample.cameraxMLsample.R
import com.fs.sample.cameraxMLsample.viewmodel.TextRecognitionViewModel
import com.fs.sample.cameraxMLsample.viewmodel.TextTranslationViewModel
import java.util.*

@ExperimentalAnimationApi
@Composable
fun BindTextRecognitionOutput(
    textRecognitionViewModel: TextRecognitionViewModel,
    textTranslationViewModel: TextTranslationViewModel,
    listener: TextRecognitionComposableInterface
) {
    val showTranslationLanguages = remember { textTranslationViewModel.showLanguagesState }
    val showTranslation = remember { textTranslationViewModel.showTranslation }

    ConstraintLayout(Modifier.fillMaxSize()) {
        val (title,
            text,
            backToCameraButton,
            transButton,
            progress,
            transLangBox) = createRefs()
        val textValue by textRecognitionViewModel.getOutput().observeAsState()
        val languages by textTranslationViewModel.supportedLanguages.observeAsState()
        val translation by textTranslationViewModel.getOutput().observeAsState()
        val scrollState = rememberScrollState(0)
        val transScrollState = rememberScrollState(0)

        Text(
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.wrapContent
                    height = Dimension.wrapContent
                }
                .then(Modifier.padding(16.dp)),
            text = stringResource(R.string.camera_output_result),
            style = TextStyle(fontStyle = FontStyle.Italic),
            fontWeight = FontWeight.Bold
        )

        SelectionContainer(modifier = Modifier
            .constrainAs(text) {
                linkTo(top = title.bottom, bottom = backToCameraButton.top, 16.dp, 16.dp)
                linkTo(start = parent.start, end = parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
            .then(
                Modifier.padding(16.dp)
            )
            .then(
                Modifier.border(
                    width = 4.dp,
                    color = colorResource(id = R.color.purple_500),
                    shape = RectangleShape
                )
            )) {
            Text(
                text = textValue ?: "",
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .then(Modifier.padding(8.dp))
            )
        }

        Button(modifier = Modifier.constrainAs(transButton) {
            bottom.linkTo(text.top, margin = (-20).dp)
            end.linkTo(parent.end, margin = 18.dp)
            width = Dimension.preferredWrapContent
            height = Dimension.preferredWrapContent
        }, onClick = {
            textTranslationViewModel.loadSupportedLanguages()
        })
        {
            Text(
                text = stringResource(R.string.translate_button)
            )
        }

        Button(modifier = Modifier.constrainAs(backToCameraButton) {
            linkTo(start = parent.start, end = parent.end)
            bottom.linkTo(parent.bottom, 16.dp)
            width = Dimension.preferredWrapContent
            height = Dimension.preferredWrapContent
        }, onClick = {
            listener.onCameraBackButtonClick()
        })
        {
            Text(stringResource(R.string.go_to_camerascreen_button))
        }

        val isLoading = remember { textTranslationViewModel.getLoadingProgress() }
        CircularProgressIndicator(
            modifier = Modifier
                .constrainAs(progress) {
                    linkTo(top = parent.top, bottom = parent.bottom)
                    linkTo(start = parent.start, end = parent.end)
                    width = Dimension.value(80.dp)
                    height = Dimension.value(80.dp)
                }
                .then(Modifier.alpha(if (isLoading.value) 1f else 0f)),
        )

        AnimatedVisibility(
            visible = showTranslationLanguages.value,
            enter = scaleIn(
                initialScale = 0.0f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(durationMillis = 1000)
            ),
            exit = scaleOut(
                targetScale = 0.0f,
                transformOrigin = TransformOrigin.Center,
                animationSpec = tween(durationMillis = 1000)
            ),
            modifier = Modifier
                .constrainAs(transLangBox) {
                    linkTo(top = parent.top, bottom = parent.bottom, 8.dp, 8.dp)
                    linkTo(start = parent.start, end = parent.end, 8.dp, 8.dp)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        ) {
            Box(
                modifier = Modifier
                    .background(
                        colorResource(id = R.color.purple_500),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .then(Modifier.padding(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = !showTranslation.value,
                    enter = fadeIn(
                        initialAlpha = 0.0f,
                        animationSpec = tween(durationMillis = 500)
                    ),
                    exit = fadeOut(
                        targetAlpha = 0.0f,
                        animationSpec = tween(durationMillis = 500)
                    )
                ) {
                    LazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages?.let { list ->
                            items(list.toList(), key = {
                                it
                            }) { lang ->
                                BuildTransLangCard(lang = lang, listener = listener)
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = showTranslation.value,
                    enter = fadeIn(
                        initialAlpha = 0.0f,
                        animationSpec = tween(durationMillis = 500)
                    ),
                    exit = fadeOut(
                        targetAlpha = 0.0f,
                        animationSpec = tween(durationMillis = 500)
                    )
                ) {
                    Text(
                        text = translation ?: "",
                        modifier = Modifier
                            .verticalScroll(transScrollState)
                            .fillMaxSize()
                            .padding(16.dp),
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }
            }
        }

    }

    BackHandler(showTranslationLanguages.value || showTranslation.value) {
        if (showTranslation.value) {
            textTranslationViewModel.showTranslation.value = false
        } else if (showTranslationLanguages.value) {
            textTranslationViewModel.showLanguagesState.value = false
        }
    }
}

@Composable
fun BuildTransLangCard(lang: String, listener: TextRecognitionComposableInterface) {
    Card(
        backgroundColor = Color.White,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.selectable(selected = false,
            onClick = { listener.onLanguageToTranslateSelected(lang) }
        )
    ) {
        Text(
            text = Locale.forLanguageTag(lang).displayName,
            color = Color.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

interface TextRecognitionComposableInterface {
    fun onCameraBackButtonClick()
    fun onLanguageToTranslateSelected(lang: String)
}