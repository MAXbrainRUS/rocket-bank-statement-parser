# rocket-bank-statement-parser

[![Build Status](https://travis-ci.org/MAXbrainRUS/rocket-bank-statement-parser.svg?branch=master)](https://travis-ci.org/MAXbrainRUS/rocket-bank-statement-parser)

# Description

"Rocket parser" is a simple program provide converting Rocket Bank's (https://rocketbank.ru/) statement from .pdf format to .xls Excel format.
It is useful for import your transactions to some programs for budget accounting.

## Category filling

"Rocket parser" provides automatic category filling on key words of transaction's description.
For this just add file `KeyWordsToCategoryMap.json` with map keyword->category.

Rules for category detecting:
1. If description of transaction contains keyword category name is filed to 'category' column.
2. If description of transaction contains more then one keyword from list, 1st one is applied. 
```json
{
  "Some key word/phrase": "Name of category",
  "Some other key word/phrase": "Name of other category"
}
```
