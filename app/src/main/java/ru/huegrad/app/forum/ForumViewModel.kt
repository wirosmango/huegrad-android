package ru.huegrad.app.forum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Topic(val id: String, val title: String)

sealed interface TopicsState {
    data object NotConfigured : TopicsState
    data object Loading : TopicsState
    data class Ready(val topics: List<Topic>) : TopicsState
    data class Error(val message: String) : TopicsState
}

class ForumViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<TopicsState>(TopicsState.Loading)
    val state: StateFlow<TopicsState> = _state.asStateFlow()

    private var registration: ListenerRegistration? = null

    init {
        if (!ForumAuth.isConfigured(app)) {
            _state.value = TopicsState.NotConfigured
        } else {
            viewModelScope.launch {
                try {
                    ForumAuth.signIn()
                } catch (e: Exception) {
                    _state.value = TopicsState.Error(ForumAuth.describe(e))
                    return@launch
                }
                registration = FirebaseFirestore.getInstance().collection("topics")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            _state.value = TopicsState.Error(err.localizedMessage ?: "Ошибка загрузки тем")
                            return@addSnapshotListener
                        }
                        _state.value = TopicsState.Ready(
                            snap?.documents.orEmpty().map { Topic(it.id, it.getString("title") ?: "Без названия") }
                        )
                    }
            }
        }
    }

    fun createTopic(title: String, onDone: (String?) -> Unit) {
        FirebaseFirestore.getInstance().collection("topics")
            .add(mapOf("title" to title.trim().take(100), "createdAt" to FieldValue.serverTimestamp()))
            .addOnCompleteListener { onDone(if (it.isSuccessful) it.result.id else null) }
    }

    override fun onCleared() {
        registration?.remove()
        super.onCleared()
    }
}