package com.isaakhanimann.journal.ui.screens.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.ai.*
import com.isaakhanimann.journal.ui.viewmodel.AIAssistantViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    viewModel: AIAssistantViewModel = koinInject()
) {
    val currentConversation by viewModel.currentConversation.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showConversationList by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(currentConversation?.messages?.size) {
        if (currentConversation?.messages?.isNotEmpty() == true) {
            coroutineScope.launch {
                listState.animateScrollToItem(currentConversation!!.messages.size - 1)
            }
        }
    }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Conversation List Sidebar
        if (showConversationList) {
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(end = 8.dp)
            ) {
                ConversationListSidebar(
                    conversations = conversationHistory,
                    currentConversationId = currentConversation?.id,
                    onConversationSelected = { conversation ->
                        viewModel.switchToConversation(conversation.id)
                        showConversationList = false
                    },
                    onNewConversation = {
                        viewModel.startNewConversation()
                        showConversationList = false
                    },
                    onDeleteConversation = viewModel::deleteConversation
                )
            }
        }
        
        // Main Chat Interface
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentConversation?.title ?: "AI Assistant",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Lucy • Harm Reduction Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showConversationList = !showConversationList }) {
                        Icon(Icons.Default.Menu, contentDescription = "Conversations")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Conversation")
                    }
                    IconButton(onClick = { 
                        currentConversation?.let { 
                            viewModel.exportConversation(it.id) 
                        } 
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            )
            
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                currentConversation?.messages?.let { messages ->
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isFromUser = message.role == MessageRole.USER
                        )
                    }
                }
                
                if (isProcessing) {
                    item {
                        TypingIndicator()
                    }
                }
            }
            
            // Message Input
            MessageInput(
                message = messageText,
                onMessageChange = { messageText = it },
                onSendMessage = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                isProcessing = isProcessing
            )
        }
    }
}

@Composable
fun ConversationListSidebar(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onConversationSelected: (Conversation) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onNewConversation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Conversation")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(conversations) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    isSelected = conversation.id == currentConversationId,
                    onClick = { onConversationSelected(conversation) },
                    onDelete = { onDeleteConversation(conversation.id) }
                )
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = "${conversation.messages.size} messages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromUser) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = "AI",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp)
            )
        }
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isFromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                // Show metadata for AI responses
                if (!isFromUser && message.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    message.metadata["confidence"]?.let { confidence ->
                        Text(
                            text = "Confidence: $confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    message.metadata["suggestions"]?.takeIf { it.isNotBlank() }?.let { suggestions ->
                        Text(
                            text = "Suggestions: $suggestions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.timestamp.toString().substring(11, 16), // Show time
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
        
        if (isFromUser) {
            Icon(
                Icons.Default.Person,
                contentDescription = "User",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = "AI",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .padding(end = 8.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            shape = CircleShape
                        ) {}
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Lucy is typing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Ask Lucy about harm reduction, experiences, or safety...") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp, max = 120.dp),
                maxLines = 5,
                enabled = !isProcessing
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            FloatingActionButton(
                onClick = onSendMessage,
                modifier = Modifier.size(48.dp),
                containerColor = if (message.isNotBlank() && !isProcessing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (message.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}