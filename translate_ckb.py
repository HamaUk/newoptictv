import xml.etree.ElementTree as ET
from deep_translator import GoogleTranslator
import time

def translate():
    try:
        tree = ET.parse('app/src/main/res/values-ckb/strings.xml')
        root = tree.getroot()
        translator = GoogleTranslator(source='en', target='ckb')

        count = 0
        for child in root.findall('string'):
            text = child.text
            if text and text.strip():
                # Skip if it has HTML tags or complex formatting
                if '<' in text or 'CDATA' in text:
                    continue
                try:
                    translated = translator.translate(text)
                    if translated:
                        child.text = translated
                        count += 1
                        if count % 50 == 0:
                            print(f"Translated {count} strings...")
                            tree.write('app/src/main/res/values-ckb/strings.xml', encoding='utf-8', xml_declaration=True)
                except Exception as e:
                    print(f"Error on {text}: {e}")
                    time.sleep(1)

        tree.write('app/src/main/res/values-ckb/strings.xml', encoding='utf-8', xml_declaration=True)
        print(f"Done! Translated {count} strings.")
    except Exception as e:
        print(f"Translation failed: {e}")

translate()
