package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AddPhotoAlternate
import com.example.BuildConfig
import com.example.R
import com.example.ai.*
import com.example.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatViewModel(
    private val repository: AppRepository
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // Maintain conversation history
    private val conversationHistory = mutableListOf<Content>()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage(text, true)
        _messages.value = _messages.value + userMsg
        conversationHistory.add(Content(listOf(Part(text))))

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = conversationHistory,
                    systemInstruction = Content(
                        listOf(
                            Part(
                                """
                                أنت طبيب بشري متخصص في التشخيص الطبي، لديك خبرة 25 عاماً في الطب الباطني والرعاية الأولية.
                                أنت تتعامل مع مرضاك بلطف، حكمة، واهتمام بالغ.

                                ## دورك الأساسي:
                                1. **جمع الأعراض بدقة**: اسأل المريض عن الأعراض الرئيسية، متى بدأت، شدتها، وما يزيدها أو يخففها.
                                2. **تحليل الأعراض**: حلل الأعراض في سياقها، وابحث عن الأنماط التي تشير إلى حالات معينة.
                                3. **التشخيص المبدئي**: قدم تشخيصاً مبدئياً مع نسب مئوية للثقة، واذكر الأسباب المحتملة.
                                4. **التوصيات العلاجية**: قدم نصائح عملية (أدوية متاحة بدون وصفة، راحة، نظام غذائي، تمارين).
                                5. **علامات الخطر**: حدد بوضوح متى يجب على المريض زيارة الطبيب فوراً أو التوجه للطوارئ.

                                ## قواعد مهنية صارمة:
                                - **حياة المريض أولاً**: إذا كانت الأعراض تشير إلى حالة طارئة (ألم صدر، ضيق تنفس، نزيف حاد، فقدان وعي) فقل بوضوح: "هذه حالة طارئة، اتصل بالإسعاف فوراً".
                                - **لا تقدم تشخيصاً نهائياً**: قل دائماً "هذا تشخيص مبدئي، يجب مراجعة طبيب مختص للتأكيد".
                                - **كن متعاطفاً**: استخدم لغة دافئة ومطمئنة، واشعر المريض بأنه مسموع.
                                - **اللغة العربية**: تكلم باللغة العربية الفصحى أو العامية المفهومة.

                                ## هيكل الرد المثالي:
                                1. **تحية دافئة**: "أهلاً بك، أنا هنا لمساعدتك..."
                                2. **جمع معلومات إضافية**: اسأل عن أي أعراض غير مذكورة.
                                3. **التحليل الأولي**: اشرح ما تفكر به بطريقة مبسطة.
                                4. **التشخيص المبدئي**: مع نسبة ثقة (مثل 70%).
                                5. **التوصيات**: نصائح عملية.
                                6. **متى تزور الطبيب**: حدد الحالات التي تستدعي التدخل الطبي.
                                7. **خاتمة مطمئنة**: ادعُ المريض للعودة بأي استفسارات.
                                """.trimIndent()
                            )
                        )
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Error: No response"
                
                conversationHistory.add(Content(listOf(Part(replyText))))
                _messages.value = _messages.value + ChatMessage(replyText, false)

                val diagnosis = com.example.data.Diagnosis(
                    symptoms = text,
                    diagnosisText = replyText,
                    recommendations = "من الرد أعلاه",
                    confidence = "غير محددة"
                )
                repository.saveDiagnosis(diagnosis)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Error: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessageWithImage(text: String, imageUri: Uri, context: Context) {
        val userText = if (text.isBlank()) "إليك هذا الفحص" else text
        val userMsg = ChatMessage(userText, true)
        _messages.value = _messages.value + userMsg
        conversationHistory.add(Content(listOf(Part(userText))))
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                val stream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, stream)
                val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                val requestContents = listOf(
                    Content(listOf(
                        Part(userText),
                        Part(inlineData = InlineData("image/jpeg", base64Image))
                    ))
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = requestContents,
                    systemInstruction = Content(listOf(Part(
                        """
                        أنت استشاري تحليل طبي وخبير صيدلة إكلينيكي.
                        دورك:
                        1. **قراءة الفحص**: استخرج جميع القيم الرقمية والمصطلحات من الصورة.
                        2. **تفسير النتائج**: قارن القيم بالنطاقات الطبيعية، واشرح ما تعنيه.
                        3. **تحليل الأشعة**: صف ما تراه في الصورة (سواد، بياض، كسور، ظلال).
                        4. **اقتراح الجرعات**: بناءً على الوزن والعمر (إذا وفرها المريض)، اقترح جرعة دواء مناسبة مع جدول زمني.
                        5. **تحذير صارم**: دائماً قل "هذا اقتراح أولي، استشر طبيبك لتأكيد الجرعة".
                        استخدم لغة عربية مبسطة ودقيقة.
                        """.trimIndent()
                    )))
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Error: No response"
                
                conversationHistory.add(Content(listOf(Part(replyText))))
                _messages.value = _messages.value + ChatMessage(replyText, false)

                val diagnosis = com.example.data.Diagnosis(
                    symptoms = userText,
                    diagnosisText = replyText,
                    recommendations = "تحليل فحص",
                    confidence = "غير محددة"
                )
                repository.saveDiagnosis(diagnosis)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("خطأ في قراءة الفحص: ${e.message}", false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class ChatViewModelFactory(private val repository: AppRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, repository: AppRepository) {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(repository))
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendMessageWithImage(inputText, it, context) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_doctor)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (isLoading) {
                    item {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_hint)) },
                    singleLine = false,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "رفع فحص")
                }
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (msg.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(12.dp),
                color = if (msg.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
