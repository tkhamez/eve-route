db.createUser({
    user: "eve-route",
    pwd: "password",
    roles: [
        { role: "readWrite", db: "eve-route" }
    ]
});
