import tsParser from "@typescript-eslint/parser"
import react from "eslint-plugin-react"

export default [
  { ignores: ["dist/**", "node_modules/**"] },
  {
    files: ["src/**/*.{ts,tsx}"],
    languageOptions: { parser: tsParser, parserOptions: { ecmaFeatures: { jsx: true }, sourceType: "module" } },
    plugins: { react },
    rules: {
      "no-any": "off",
      "no-unused-vars": "off",
      "react/no-multi-comp": "off",
      "max-lines": ["warn", { max: 120, skipBlankLines: true, skipComments: true }],
      "max-lines-per-function": ["warn", { max: 40, skipBlankLines: true, skipComments: true }],
      "no-restricted-syntax": ["error", { selector: "BinaryExpression[operator='==='][left.object.property.name='role']", message: "Use domain permissions helpers for role access." }],
    },
  },
]
