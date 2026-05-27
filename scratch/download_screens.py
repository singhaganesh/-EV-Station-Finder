import urllib.request
import os

urls = {
    "1_design_system.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwYmFmNGYwYjNlZDQ2ZTFiZDNiOGIzZmJkOGRjZWFkEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086", # Wait, Map Home is 5c397... let's mapping correctly
    "write_review.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzIyMTdmMWFkZWYxZTQ3MjliOWM4ZTYzOTgxZDAxOWE5EgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "route_planner.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2QxMTQ2OTEwOWVhNDQxOGY5ZWQ3NjFhMjg2ZTM3YzhhEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "ev_station_finder_main.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2I4MDlkODllYmMzMjQwMGU4MTljMDFmYjUzZjQxMDE4EgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "station_details.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2U1YWJhMTk5ZDNlZTQ3MmU4MjRlYTcxMzAyMDMyOTMyEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "map_filtered.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2UyNWEzNzEzMjY1YzRjOWY4MTkyZTMzZTIwY2FiN2I2EgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "map_home.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzAwYmFmNGYwYjNlZDQ2ZTFiZDNiOGIzZmJkOGRjZWFkEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "station_list.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2ZhMWQ4MWM3MzkxODRlYTFiZGM2NDJhOTFiYjVjZGIyEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "ev_profile_setup.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sXzU5NDUyNTMyMDM3ZjQ0MTY5MTBkOTg1ZDNlNTE0OTQyEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086",
    "saved_stations_empty.html": "https://contribution.usercontent.google.com/download?c=CgthaWRhX2NvZGVmeBJ8Eh1hcHBfY29tcGFuaW9uX2dlbmVyYXRlZF9maWxlcxpbCiVodG1sX2IwMDQyMDg4ZTdmZjQ3ZDhhOTI1MDlhMTFiYjBjZjQyEgsSBxCW6JPcogMYAZIBJAoKcHJvamVjdF9pZBIWQhQxMzA5NDI5ODg3NDkwNjY5MjY4Mg&filename=&opi=89354086"
}

output_dir = "D:/Ganesh/work/EV-Station-Finder/stitch_screens"
os.makedirs(output_dir, exist_ok=True)

headers = {'User-Agent': 'Mozilla/5.0'}

for name, url in urls.items():
    print(f"Downloading {name}...")
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as response:
            with open(os.path.join(output_dir, name), 'wb') as out_file:
                out_file.write(response.read())
        print(f"Downloaded {name} successfully.")
    except Exception as e:
        print(f"Error downloading {name}: {e}")
