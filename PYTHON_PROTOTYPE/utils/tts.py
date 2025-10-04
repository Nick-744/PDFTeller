try:
    from utils.process_pdf import process_pdf_text_with_structure
except ImportError:
    from process_pdf import process_pdf_text_with_structure

from pathlib import Path
from time import sleep
import pyttsx3

def speak_text(text: str) -> None:
    engine = pyttsx3.init()
    engine.setProperty('rate', 225)

    engine.say(text)
    engine.runAndWait()

    return;

def main():
    base_path = Path(__file__).parent.parent.parent # Go up 2 directories!
    text      = process_pdf_text_with_structure(
        base_path / 'DATA' / 'Understanding_Climate_Change.pdf'
    )
    print(f'Number of sentences: {len(text)}\n')

    for sentence in text:
        print(sentence)
        speak_text(sentence)

        sleep(0.1)

    return;

if __name__ == '__main__':
    main()
