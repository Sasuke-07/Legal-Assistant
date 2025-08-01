# Legal Assistant Web Application

A comprehensive web based legal consultation platform that provides users with personalized legal guidance through AI-powered analysis. The application helps users understand their legal situations and provides structured advice including applicable laws, case descriptions, and recommended action steps.

## 🚀 Features

- **AI-Powered Legal Analysis**: Integration with OpenAI GPT and Google Gemini APIs for intelligent legal consultation
- **User Authentication**: Secure login and registration system with session management
- **Interactive Dashboard**: User-friendly interface to submit queries and view consultation history
- **Real-time Query Processing**: Instant legal advice generation with structured responses
- **Fallback Mechanisms**: Offline functionality when external APIs are unavailable
- **Responsive Design**: Modern, mobile-friendly web interface
- **Comprehensive Logging**: Activity tracking and error handling for debugging

## 🛠️ Technology Stack

- **Backend**: Java, JSP (JavaServer Pages), Servlets
- **Frontend**: HTML5, CSS3, JavaScript
- **APIs**: OpenAI GPT API, Google Gemini API
- **Server**: Apache Tomcat
- **Database**: File-based storage for user data and session management
- **Build Tool**: Maven (if applicable)

## 📁 Project Structure

```
legalassist/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── legalassist/
│       │           ├── servlets/
│       │           ├── models/
│       │           └── utils/
│       └── webapp/
│           ├── WEB-INF/
│           ├── css/
│           ├── js/
│           └── jsp/
├── logs/
├── data/
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- Java JDK 8 or higher
- Apache Tomcat 9.0 or higher
- IDE (Eclipse, IntelliJ IDEA, or VS Code)
- OpenAI API Key (optional)
- Google Gemini API Key (optional)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/legalassist.git
   cd legalassist
   ```

2. **Set up API Keys**
   - Create a `config.properties` file in the `src/main/resources` directory
   - Add your API keys:
     ```properties
     openai.api.key=your_openai_api_key_here
     gemini.api.key=your_gemini_api_key_here
     ```

3. **Deploy to Tomcat**
   - Copy the project to your Tomcat `webapps` directory
   - Start Tomcat server
   - Access the application at `http://localhost:8080/legalassist`

### Configuration

- Update database connection settings in the configuration files
- Modify API endpoints and timeout settings as needed
- Customize logging levels in the logging configuration

## 💻 Usage

1. **Registration/Login**: Create an account or login with existing credentials
2. **Submit Query**: Enter your legal question or situation in the query form
3. **Get Analysis**: Receive AI-powered legal advice with:
   - Applicable laws and regulations
   - Case description and analysis
   - Recommended action steps
   - Relevant legal precedents
4. **View History**: Access your previous consultations through the dashboard

## 🔧 API Integration

The application integrates with multiple AI services:

- **OpenAI GPT**: Primary AI service for legal analysis
- **Google Gemini**: Fallback service for enhanced reliability
- **Graceful Degradation**: Offline mode when APIs are unavailable

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This application provides general legal information and should not be considered as professional legal advice. Always consult with a qualified attorney for specific legal matters.

## 📞 Contact

Your Name - your.email@example.com

Project Link: [https://github.com/yourusername/legalassist](https://github.com/yourusername/legalassist)

## 🙏 Acknowledgments

- OpenAI for GPT API
- Google for Gemini API
- Apache Tomcat community
- Java servlet technology contributors
