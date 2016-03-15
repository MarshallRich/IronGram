function getUser(userDate) {
    if (userDate.length == 0) {
        $("#login").show();
    }
}

$.get("/user", getUser);