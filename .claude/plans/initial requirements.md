# Mappics V3 

I want to build a map based photo gallery that, starting from a storage bucket with pictures taken all over the world,
with geolocation stored in them, is able to fetch information of the pictures from different sources, and 
visualize them in a very simple, folder based gallery of galleries of pictures.

I started building this in the past https://github.com/antodippo/mappics-v2, so you can heavily take inspiration from it.

## Functional requirements

- given a bucket with JPG pictures, organized by location folders
    - resize them to then have thumbnails and full size picture to visualize in the gallery, choose proper size and don't increase the size
    - fetch and store the GPS location, to then visualize it on a map in the UI
    - extract some nice EXIF info
    - fetch the name and short description of the place (see what service I've used and suggest another if you find better)
    - fetch the weather in time and place the picture was taken
    - this process should be idempotent (and skip picture that already have info, and only try to fill missing info)
- expose pictures info through a simple REST API to the frontend
- the frontend should
    - have on the main page an interactive map with the an icon for each gallery, in the place where the pictures were taken (calculate the average position during the import) with a link to the gallery
    - each gallery should have an interactive map with all the picture on the place they were taken, and clicking on them a overlay should open
    - in the overlay there should be the full picture and on its right side a panel with the info (location description, EXIF, weather)
    - below the map there should be a simple gallery of thumbnails, and clicking on them the overlay should open

## Non functional requirements

- backend should be written in Java
    - it should use principles of DDD and hexagonal architecture
    - it should be test driven, 
        - using test doubles over mocking
        - testing behaviour and not implementation
        - see how I tested in the previous version

- it should be runnable locally, with stubs and/or using the real services
- it should live in Google Cloud
    - write also Terraform code and instructions to deploy
    - it should use Google Cloud Storage buckets
    - it should use the simplest possible database
- code should be in Github and continuously deployed on merge on master branch
    - use Github actions, or suggest a better options
    - same goes for infra, should be applied every time is merged
