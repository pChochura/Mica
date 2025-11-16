package com.pointlessapps.mica.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.pointlessapps.granite.mica.Mica
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mica.composeapp.generated.resources.Res
import mica.composeapp.generated.resources.app_name
import mica.composeapp.generated.resources.ic_play
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val highlightsBuilder = Highlights.Builder(
    language = SyntaxLanguage.DEFAULT,
    theme = SyntaxThemes.pastel(true),
)

@Composable
@Preview
fun App() {
    var isLoading by remember { mutableStateOf(false) }
    var isAcceptingInput by remember { mutableStateOf(false) }
    var outputState by remember { mutableStateOf(listOf<String>()) }
    val inputState = rememberTextFieldState()
    val coroutineScope = rememberCoroutineScope()
    val inputChannel = remember { Channel<String>() }
    val mica = remember {
        Mica(
            onOutputCallback = {
                outputState += "> $it"
            },
            onInputCallback = {
                isAcceptingInput = true
                inputChannel.receive()
            },
        )
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(240, 240, 240),
            primary = Color(94, 52, 106),
            primaryContainer = Color(59, 59, 105),
            onPrimaryContainer = Color(230, 230, 230),
        ),
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Editor(
                modifier = Modifier.weight(2f),
                inputState = inputState,
                onBuildClicked = {
                    coroutineScope.launch {
                        isLoading = true
                        outputState = emptyList()
                        mica.execute(inputState.text.toString())
                        isLoading = false
                    }
                },
            )
            Output(
                modifier = Modifier.weight(1f),
                outputState = outputState,
                isLoading = isLoading,
                isAcceptingInput = isAcceptingInput,
                onInputCallback = {
                    inputChannel.trySend(it)
                    isAcceptingInput = false
                },
            )
        }
    }
}

@Composable
private fun Editor(
    inputState: TextFieldState,
    onBuildClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.app_name),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp,
            )
            IconButton(
                onClick = onBuildClicked,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_play),
                    contentDescription = "Build and run",
                    tint = Color(42, 84, 25),
                )
            }
        }
        EditText(
            inputState = inputState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        )
    }
}

@Composable
private fun EditText(
    inputState: TextFieldState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (inputState.text.isEmpty()) {
            Text(
                text = "Write your code here",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
                    fontSize = 18.sp,
                    lineHeight = 1.4.em,
                    lineHeightStyle = LineHeightStyle.Default,
                ),
            )
        }

        BasicTextField(
            modifier = Modifier.fillMaxSize(),
            state = inputState,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 18.sp,
                lineHeight = 1.4.em,
                lineHeightStyle = LineHeightStyle.Default,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
                showKeyboardOnFocus = true,
            ),
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 5),
            outputTransformation = OutputTransformation {
                highlightsBuilder
                    .code(inputState.text.toString())
                    .build()
                    .getHighlights()
                    .filterIsInstance<ColorHighlight>().fastForEach {
                        addStyle(
                            spanStyle = SpanStyle(color = Color(it.rgb).copy(1f)),
                            start = it.location.start,
                            end = it.location.end,
                        )
                    }
            }
        )
    }
}

@Composable
private fun Output(
    outputState: List<String>,
    isLoading: Boolean,
    isAcceptingInput: Boolean,
    onInputCallback: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(outputState) { line ->
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = line,
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }

        if (isLoading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (isAcceptingInput) {
            item {
                var input by remember { mutableStateOf("") }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = input,
                    onValueChange = { input = it },
                    maxLines = 1,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    placeholder = @Composable {
                        Text(
                            text = "Provide input...",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            ),
                        )
                    },
                    keyboardActions = KeyboardActions { onInputCallback(input) },
                )
            }
        }
    }
}
