
/*

This script is the first of two scripts responsible for setting up mongoDB on initial boot. 
These scripts should be copied to the /docker-entrypoint-initdb.d/ directory within the docker image;
the docker image will then execute these scripts automatically when the database is first created. 
This script and is partner are prefixed with the letters a and b respectively to ensure they are run 
in the proper order when copied into the mongoDB docker image.

Documentation on how the mongoDB docker image runs startup scripts can be found here
https://hub.docker.com/_/mongo/

*/



console.log("Initializing Replicas");
try{
    db_status = rs.status();
} catch(err){
    console.log("Initializing New DB");
    try{
        rs.initiate({ _id: 'rs0', members: [{ _id: 0, host: 'localhost:27017' }] }).ok 
    }catch(err){
        console.log("Unable to Initialize DB");
    }
}
