# rocket-bank-statement-parser

"Rocket parser" is a simple program provide converting Rocket Bank's (https://rocketbank.ru/) statement from .pdf format to .xls Excel format.
It useful for import your transaction to some programs for budget accounting.

It also provide automatic category filling on key words of transaction's description.
For this just add file `KeyWordsToCategoryMap.json` with map keyword->category
```json
{
  "Some key word/phrase": "Name of category",
  "Some other key word/phrase": "Name of category",
}
```
