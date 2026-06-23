package validator

import (
	"strconv"
	"strings"
)

// IsValidCPFCNPJ validates a Brazilian CPF (11 digits) or CNPJ (14 digits).
// This replaces the Caelum Stella library used in the Java version.
func IsValidCPFCNPJ(value string) bool {
	// Strip non-digits
	digits := stripNonDigits(value)

	switch len(digits) {
	case 11:
		return isValidCPF(digits)
	case 14:
		return isValidCNPJ(digits)
	default:
		return false
	}
}

// StripNonDigits removes all non-digit characters from a string.
// Used for normalizing CPF/CNPJ input (equivalent to Java's replaceAll("\\D", "")).
func StripNonDigits(value string) string {
	return stripNonDigits(value)
}

func stripNonDigits(s string) string {
	var b strings.Builder
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
}

// isValidCPF validates a Brazilian CPF using the standard check-digit algorithm.
func isValidCPF(cpf string) bool {
	if len(cpf) != 11 {
		return false
	}

	// Reject known invalid sequences (all same digits)
	if allSameDigits(cpf) {
		return false
	}

	// Validate first check digit
	sum := 0
	for i := 0; i < 9; i++ {
		d, _ := strconv.Atoi(string(cpf[i]))
		sum += d * (10 - i)
	}
	remainder := (sum * 10) % 11
	if remainder == 10 {
		remainder = 0
	}
	d10, _ := strconv.Atoi(string(cpf[9]))
	if remainder != d10 {
		return false
	}

	// Validate second check digit
	sum = 0
	for i := 0; i < 10; i++ {
		d, _ := strconv.Atoi(string(cpf[i]))
		sum += d * (11 - i)
	}
	remainder = (sum * 10) % 11
	if remainder == 10 {
		remainder = 0
	}
	d11, _ := strconv.Atoi(string(cpf[10]))
	return remainder == d11
}

// isValidCNPJ validates a Brazilian CNPJ using the standard check-digit algorithm.
func isValidCNPJ(cnpj string) bool {
	if len(cnpj) != 14 {
		return false
	}

	if allSameDigits(cnpj) {
		return false
	}

	// Weights for CNPJ validation
	weights1 := []int{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
	weights2 := []int{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}

	// Validate first check digit
	sum := 0
	for i := 0; i < 12; i++ {
		d, _ := strconv.Atoi(string(cnpj[i]))
		sum += d * weights1[i]
	}
	remainder := sum % 11
	checkDigit1 := 0
	if remainder >= 2 {
		checkDigit1 = 11 - remainder
	}
	d13, _ := strconv.Atoi(string(cnpj[12]))
	if checkDigit1 != d13 {
		return false
	}

	// Validate second check digit
	sum = 0
	for i := 0; i < 13; i++ {
		d, _ := strconv.Atoi(string(cnpj[i]))
		sum += d * weights2[i]
	}
	remainder = sum % 11
	checkDigit2 := 0
	if remainder >= 2 {
		checkDigit2 = 11 - remainder
	}
	d14, _ := strconv.Atoi(string(cnpj[13]))
	return checkDigit2 == d14
}

func allSameDigits(s string) bool {
	for i := 1; i < len(s); i++ {
		if s[i] != s[0] {
			return false
		}
	}
	return true
}
