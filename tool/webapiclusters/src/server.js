// https://www.w3resource.com/node.js/nodejs-sqlite.php
// https://www.hongkiat.com/blog/node-js-server-side-javascript/
// https://ponyfoo.com/articles/understanding-javascript-async-await


var sys = require("sys"),
    my_http = require("http"),
    url = require("url"),
    csv = require('csv-string'),
    fs = require('fs');


var sqlite3 = require('sqlite3').verbose();

//const dblocation = "C:/Data/Papers/apiclustering/derived/database.db";
const results = "C:/Data/Repos/apiclustering/dataset/Results.csv";

// // Deleting previous database.
// fs.unlink(dblocation, function (error) {
//     if (error) {
//         throw error;
//     }
// });

var db = new sqlite3.Database(':memory:');

db.run("CREATE TABLE configurations (\n " +
    "id integer NOT NULL,\n " +
    "key text NOT NULL,\n " +
    "value text NOT NULL,\n " +
    "kind text NOT NULL);");

// db.run("CREATE TABLE nodes (\n " +
//     "configuration integer NOT NULL,\n " +
//     "name text NOT NULL);");

db.run("CREATE TABLE linkage (\n " +
    "configuration integer NOT NULL,\n " +
    "json text NOT NULL);");

// db.run("CREATE TABLE linkage (\n " +
//     "configuration integer NOT NULL,\n " +
//     "source text NOT NULL,\n " +
//     "target text NOT NULL,\n " +
//     "similarity real NOT NULL);");


fs.readFile(results, 'utf8', function (err, data) {
    if (err) throw err;

    var header = null;

    csv.forEach(data, ',', '"', function (row, index) {
        if (header == null) {
            header = {};
            var i;
            for (i = 0; i < row.length; i++) {
                if (!row[i].startsWith("#"))
                    header[row[i]] = i;
            }
        } else {
            configuration_index = row[header["configuration_index"]];
            if (configuration_index != undefined) {
                console.log('inserting configuration', configuration_index);
                // Inserting different features assignments.
                insert = "INSERT INTO configurations ( id, key, value, kind) VALUES ";
                for (var column in header) {
                    if (column == "linkage") continue;
                    if (column == "apis") continue;
                    if (column == "configuration_index") continue;

                    insert = insert + "(" + configuration_index + ",'" + column + "','" + row[header[column]] + "','" + "input" + "'),"
                }
                db.run(insert.substring(0, insert.length - 1));

                // Inserting linkage.
                let linkage = row[header['linkage']].split(" ");
                let apis = row[header['apis']].split(";");

                let dict = {};
                for (i = 0; i < linkage.length; i++) {
                    let elements = linkage[i].split(";")
                    dict[apis.length + linkage.length - i -1] ={
                        'left':elements[0],
                        'right':elements[1],
                        'threshold':elements[2],
                        'size':elements[2],
                        'name':'',
                        'type':'node'
                    }
                }

                for (i = 0; i < apis.length; i++) {
                    dict[i] ={
                        'threshold':'0',
                        'size':'1',
                        'name':apis[i],
                        'type':'leaf'
                    }
                }

                function toJson(index) {
                    let x = dict[index];

                    if(x['type'] === 'node') x['children'] = [toJson(x['right']),toJson(x['left'])];
                    return x;
                }

                db.run("INSERT INTO linkage ( configuration, json) VALUES ($configuration, $json)", {
                    $configuration: configuration_index,
                    $json: JSON.stringify(toJson(apis.length + linkage.length -1))
                });
            }
        }
    });
});


function respondInputParameter(response) {
    //let db = new sqlite3.Database(dblocation);
    let sql = "SELECT DISTINCT key,value FROM configurations WHERE kind = 'input';";
    db.all(sql, function (err, rows) {
        var data = {};
        rows.forEach(function (row) {
            if (data[row.key] == null)
                data[row.key] = [row.value];
            else
                data[row.key].push(row.value);
        });

        response.writeHeader(200, {
            "Content-Type": "application/jsons",
            "Access-Control-Allow-Origin": "*"
        });
        response.write(JSON.stringify(data));
        response.end();
        //db.close()
    });
}


function respondData(response, query) {
    // TODO: Do safe!
    let sql_configs = "SELECT * FROM " + Object.keys(query).map((k) => "(SELECT id FROM configurations WHERE key = '" + k + "' AND value = '" + query[k] + "')").join(" NATURAL JOIN ") + ";";

    console.log(sql_configs);

    // var linkage = null;

    //let db = new sqlite3.Database(dblocation);
    //
    // function sent() {
    //     if (linkage != null) {
    //         response.writeHeader(200, {
    //             "Content-Type": "application/jsons",
    //             "Access-Control-Allow-Origin": "*"
    //         });
    //         response.write(JSON.stringify({linkage: linkage}));
    //         response.end();
    //         //db.close();
    //     }
    // }

    db.all(sql_configs, function (err, rows) {
        if (rows != undefined && rows.length > 0) {
            // Call the queries for edges and nodes.
            id = rows[0].id;
            db.all("SELECT json FROM linkage WHERE configuration = '" + id + "';", (err, rows) => {
                response.writeHeader(200, {
                    "Content-Type": "application/jsons",
                    "Access-Control-Allow-Origin": "*"
                });
                let r =  rows[0]['json']
                response.write(JSON.stringify({linkage: JSON.parse(r)}));
                response.end();
            });
            // db.all("SELECT * FROM nodes WHERE configuration = '" + id + "';", (err, rows) => {
            //     nodes = rows;
            //     sent();
            // });
        }
        // else
        //     db.close();
    });
}

my_http.createServer(function (request, response) {
    let query = url.parse(request.url, true).query;
    if (query.tpe === 'parameter')
        respondInputParameter(response)
    else
        respondData(response, query)
}).listen(8080);

sys.puts("Server Running on 8080");