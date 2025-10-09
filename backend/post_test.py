import requests

baseURL = 'http://localhost:8000'

url = f'{baseURL}/api/process_pdf'
pdf = {'pdf_file': open('./DATA/Understanding_Climate_Change.pdf', 'rb')}

response = requests.post(url, files = pdf)
for sentence in response.json()[:5]:
    print(sentence)

# Return all pdf library entries
url      = f'{baseURL}/api/library'
response = requests.get(url)
print(f'\nLibrary Entries: {response.json()}')
