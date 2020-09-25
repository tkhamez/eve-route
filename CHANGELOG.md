
## next

yyyy-mm-dd

- Fixed alliance check during login.
- Added CSRF protection for all requests that change data.
- Added option to enable the "secure" flag for the session cookie.
- More efficient storage of Ansiblex gates in the database.
- Added database migrations.
- Small layout improvements.
- Some cleanups, library updates and improved documentation.

## 0.2.0

2020-09-20

- [#3][i3] Added support for PostgreSQL, MySQL, MariaDB, SQLite and H2 (embedded mode) databases.
- User is logged off from the frontend if the backend responds with a "Forbidden" error.
- Fixed some MongoDB operations.

[i3]: https://github.com/tkhamez/eve-route/issues/3

## 0.1.0

2020-09-19

First release.
