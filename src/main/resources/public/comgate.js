function validateEmail() {
//    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    if (/^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/.test(form.email.value)) {
        return (true)
    }
    alert("Zadaná emailová adresa není validní!")
    return (false)
}


   