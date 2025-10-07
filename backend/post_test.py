import requests

url = 'http://localhost:8000/api/process_pdf'
pdf = {'pdf_file': open('./DATA/Understanding_Climate_Change.pdf', 'rb')}

response = requests.post(url, files = pdf)
for sentence in response.json()[:5]:
    print(sentence)
