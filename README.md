# <samp>nbtscript</samp>

[![test](https://github.com/intsuc/nbtscript/actions/workflows/test.yml/badge.svg)](https://github.com/intsuc/nbtscript/actions/workflows/test.yml)

<samp>nbtscript</samp> is an NBT-based programming language.

## Features

- [x] NBTs as primitive data types
- [x] Textual representation
  - [x] LSP support
    - [x] Hover
    - [x] Inlay Hint
    - [x] Completion
    - [x] Push Diagnostics
    - [ ] ...
- [x] Dependent types
- [ ] Multi-level types
- [ ] ...

## Example

```hs
fun zero: int = 0;
$(
  let id: (α: universe) -> α -> α = (α) => (a) => a;
  let id_int = id(int);
  let zero = id_int(0);

  let id = (α: type) => (a: code_z $α) => a;
  let id_code_z_int = id(`z int);
  let quote_of_zero = id_code_z_int(`z 0);

  let quote_of_quote_of_zero: code_s code_z int = `s `z 0;
  let splice_of_quote_of_quote_of_zero: code_z int = $quote_of_quote_of_zero;

  quote_of_zero
)
```

## References

- András Kovács. 2022. Staged Compilation with Two-Level Type Theory.
