
class JirafChatBot {
	constructor() {
		this.messageInput = document.getElementById('messageInput');
		this.sendButton = document.getElementById('sendButton');
		this.chatMessages = document.getElementById('chatMessages');

		this.initializeEventListeners();
		this.adjustTextareaHeight();
	}

	initializeEventListeners() {
		// Send button click
		this.sendButton.addEventListener('click', () => this.sendMessage());

		// Enter key to send (Shift+Enter for new line)
		this.messageInput.addEventListener('keydown', (e) => {
			if (e.key === 'Enter' && !e.shiftKey) {
				e.preventDefault();
				this.sendMessage();
			}
		});

		// Auto-resize textarea
		this.messageInput.addEventListener('input', () => this.adjustTextareaHeight());

		// Navigation items
		document.querySelectorAll('.nav-item').forEach(item => {
			item.addEventListener('click', () => {
				document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
				item.classList.add('active');
			});
		});
	}

	adjustTextareaHeight() {
		this.messageInput.style.height = 'auto';
		this.messageInput.style.height = Math.min(this.messageInput.scrollHeight, 120) + 'px';
	}

	sendMessage() {
		const message = this.messageInput.value.trim();
		if (!message) return;

		// Add user message
		this.addMessage(message, 'user');

		// Clear input after sending
		this.messageInput.value = '';
		this.adjustTextareaHeight();

		// Show typing indicator
		this.showTypingIndicator();

		// Send to backend
		this.sendToBackend(message);
	}

	addMessage(text, sender) {
		const messageDiv = document.createElement('div');
		messageDiv.className = `message ${sender}`;

		const avatar = document.createElement('div');
		avatar.className = 'message-avatar';
		avatar.textContent = sender === 'user' ? '👤' : '🤖';

		const content = document.createElement('div');
		content.className = 'message-content';
		content.innerHTML = this.formatMessage(text);

		const time = document.createElement('div');
		time.className = 'message-time';
		time.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		content.appendChild(time);

		messageDiv.appendChild(avatar);
		messageDiv.appendChild(content);

		// Remove welcome message if it exists
		const welcomeMessage = this.chatMessages.querySelector('.welcome-message');
		if (welcomeMessage) {
			welcomeMessage.remove();
		}

		this.chatMessages.appendChild(messageDiv);
		this.scrollToBottom();
	}

	formatMessage(text) {
		// Basic formatting for code blocks and lists
		return text
			.replace(/```([\s\S]*?)```/g, '<pre style="background: #f1f5f9; padding: 10px; border-radius: 8px; margin: 10px 0; overflow-x: auto;"><code>$1</code></pre>')
			.replace(/`([^`]+)`/g, '<code style="background: #f1f5f9; padding: 2px 6px; border-radius: 4px; font-family: monospace;">$1</code>')
			.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
			.replace(/\*(.*?)\*/g, '<em>$1</em>')
			.replace(/\n/g, '<br>');
	}

	showTypingIndicator() {
		const typingDiv = document.createElement('div');
		typingDiv.className = 'message bot';
		typingDiv.innerHTML = `
                    <div class="message-avatar">🤖</div> <div class="typing-indicator" style="display: block;">
                        <div class="typing-dots">
                            <div class="typing-dot"></div>
                            <div class="typing-dot"></div>
                            <div class="typing-dot"></div>
                        </div>
                    </div>
                `;

		this.chatMessages.appendChild(typingDiv);
		this.scrollToBottom();

		return typingDiv;
	}

	hideTypingIndicator(typingElement) {
		if (typingElement) {
			typingElement.remove();
		}
	}

	scrollToBottom() {
		this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
	}

	async sendToBackend(message) {
		const typingElement = this.chatMessages.lastElementChild;

		try {
			// Get or generate session ID
			let sessionId = localStorage.getItem('jiraf-session-id');
			if (!sessionId) {
				sessionId = 'web_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
				localStorage.setItem('jiraf-session-id', sessionId);
			}

			const response = await fetch('/api/chat', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-Session-ID': sessionId
				},
				body: JSON.stringify({
					message: message,
					sessionId: sessionId
				})
			});

			if (response.status === 429) {
				this.hideTypingIndicator(typingElement);
				this.addMessage('⚠️ Too many requests. Please wait a moment before sending another message.', 'bot');
				return;
			}

			if (!response.ok) {
				throw new Error(`HTTP ${response.status}: ${response.statusText}`);
			}

			const data = await response.json();

			// Hide typing indicator
			this.hideTypingIndicator(typingElement);

			// Handle different response types
			if (data.type === 'chart' && data.chartUrl) {
				this.addChartMessage(data.response, data.chartUrl);
			} else if (data.type === 'summary' && data.downloadUrl) {
				this.addSummaryMessage(data.response, data.downloadUrl);
			} else {
				this.addMessage(data.response, 'bot');
			}

		} catch (error) {
			console.error('Error:', error);

			// Hide typing indicator
			this.hideTypingIndicator(typingElement);

			// Show error message
			this.addMessage('❌ Sorry, I encountered an error processing your request. Please try again in a moment.', 'bot');
		}
	}

	addChartMessage(text, chartUrl) {
		const messageDiv = document.createElement('div');
		messageDiv.className = 'message bot';

		const avatar = document.createElement('div');
		avatar.className = 'message-avatar';
		avatar.textContent = '🤖';

		const content = document.createElement('div');
		content.className = 'message-content';

		// Add text response
		const textDiv = document.createElement('div');
		textDiv.innerHTML = this.formatMessage(text);
		content.appendChild(textDiv);

		// Add chart image
		const chartDiv = document.createElement('div');
		chartDiv.style.cssText = 'margin-top: 10px; text-align: center;';

		// Add loading indicator
		const loadingDiv = document.createElement('div');
		loadingDiv.innerHTML = '⏳ Loading chart...';
		loadingDiv.style.cssText = 'color: #64748b; font-style: italic; padding: 20px;';
		chartDiv.appendChild(loadingDiv);

		const chartImg = document.createElement('img');
		chartImg.src = chartUrl;
		chartImg.style.cssText = 'max-width: 100%; height: auto; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: none;';
		chartImg.alt = 'Generated Chart';

		console.log('🔗 Loading chart from URL:', chartUrl);

		// Handle successful image load
		chartImg.onload = () => {
			console.log('✅ Chart loaded successfully');
			loadingDiv.style.display = 'none';
			chartImg.style.display = 'block';
			chartImg.style.opacity = '0';
			chartImg.style.transition = 'opacity 0.3s ease';
			setTimeout(() => {
				chartImg.style.opacity = '1';
			}, 100);
		};

		// Handle image load error
		chartImg.onerror = () => {
			console.error('❌ Failed to load chart from:', chartUrl);
			loadingDiv.innerHTML = '❌ Error loading chart';
			loadingDiv.style.color = '#ef4444';

			// Try to fetch the chart URL to see what's happening
			fetch(chartUrl)
				.then(response => {
					console.log('Chart URL response status:', response.status);
					if (!response.ok) {
						console.error('Chart URL returned status:', response.status, response.statusText);
					}
				})
				.catch(error => {
					console.error('Error fetching chart URL:', error);
				});
		};

		chartDiv.appendChild(chartImg);
		content.appendChild(chartDiv);

		const time = document.createElement('div');
		time.className = 'message-time';
		time.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		content.appendChild(time);

		messageDiv.appendChild(avatar);
		messageDiv.appendChild(content);

		this.chatMessages.appendChild(messageDiv);
		this.scrollToBottom();
	}

	addSummaryMessage(text, downloadUrl) {
		const messageDiv = document.createElement('div');
		messageDiv.className = 'message bot';

		const avatar = document.createElement('div');
		avatar.className = 'message-avatar';
		avatar.textContent = '🤖';

		const content = document.createElement('div');
		content.className = 'message-content';

		// Add text response
		const textDiv = document.createElement('div');
		textDiv.innerHTML = this.formatMessage(text);
		content.appendChild(textDiv);

		// Add download section
		const downloadDiv = document.createElement('div');
		downloadDiv.className = 'download-section';

		const downloadButton = document.createElement('a');
		downloadButton.className = 'download-button';
		downloadButton.href = downloadUrl;
		downloadButton.download = '';  // This will use the filename from the server
		downloadButton.innerHTML = `
                    <span>📁</span>
                    <span>Download Complete Results</span>
                `;

		// Add click handler for download tracking and animation
		downloadButton.addEventListener('click', (e) => {
			console.log('📥 Starting download from:', downloadUrl);

			// Add success animation
			downloadButton.classList.add('download-success');
			setTimeout(() => {
				downloadButton.classList.remove('download-success');
			}, 600);

			// Update button text temporarily
			const originalHTML = downloadButton.innerHTML;
			downloadButton.innerHTML = `
                        <span>⏳</span>
                        <span>Preparing Download...</span>
                    `;

			setTimeout(() => {
				downloadButton.innerHTML = `
                            <span>✅</span>
                            <span>Download Started</span>
                        `;

				setTimeout(() => {
					downloadButton.innerHTML = originalHTML;
				}, 2000);
			}, 1000);
		});

		const downloadInfo = document.createElement('div');
		downloadInfo.className = 'download-info';
		downloadInfo.textContent = 'Complete results will be downloaded as a formatted text file';

		downloadDiv.appendChild(downloadButton);
		downloadDiv.appendChild(downloadInfo);
		content.appendChild(downloadDiv);

		const time = document.createElement('div');
		time.className = 'message-time';
		time.textContent = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
		content.appendChild(time);

		messageDiv.appendChild(avatar);
		messageDiv.appendChild(content);

		this.chatMessages.appendChild(messageDiv);
		this.scrollToBottom();
	}
}

// Initialize the chat bot when the page loads
document.addEventListener('DOMContentLoaded', () => {
	new JirafChatBot();
});
