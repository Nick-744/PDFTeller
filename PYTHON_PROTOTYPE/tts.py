from process_pdf import process_pdf_text_with_structure
from time import sleep
import pyttsx3

def speak_text(text: str) -> None:
    engine = pyttsx3.init()
    engine.setProperty('rate', 225)

    engine.say(text)
    engine.runAndWait()

    return;

def main():
    text = process_pdf_text_with_structure('Understanding_Climate_Change.pdf')
    print(f'Number of sentences: {len(text)}\n')

    for sentence in text:
        print(sentence)
        speak_text(sentence)

        sleep(0.1)

    return;

if __name__ == '__main__':
    main()
