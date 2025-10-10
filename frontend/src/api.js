import axios from 'axios'

// Create an instance of axios with a base URL
const api = axios.create({
    baseURL: 'http://192.168.1.21:8000',
})

// Bookmark function
export const addBookmark = async (pdfId, sentenceIndex) => {
    return await api.put(`/api/library/${pdfId}/bookmark?sentence_index=${sentenceIndex}`);
}

// Export the Axios instance
export default api;
