"""Host-side SQL contract check against the supplied Mixify v39 Room schema.
Does not run the Android importer or replace its instrumentation tests.
Usage: python3 tools/check_legacy_import_sql.py /path/to/39.json
"""
import json,re,sqlite3,sys
from pathlib import Path
schema=json.loads(Path(sys.argv[1]).read_text())['database']
assert schema['version']==39
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/mixify/music/data/migration/LegacyMixifyLibraryImport.kt').read_text()
sql=re.search(r'SONGS_SQL = """(.*?)"""',source,re.S)[1]
db=sqlite3.connect(':memory:');db.row_factory=sqlite3.Row
for e in schema['entities']:db.execute(e['createSql'].replace('${TABLE_NAME}',e['tableName']))
for song,title,liked,local,episode in [('aaaaaaaaaaa','First',1,0,0),('bbbbbbbbbbb','No artist',0,0,0),('ccccccccccc','Local',1,1,0),('ddddddddddd','Podcast',1,0,1)]:
 db.execute('INSERT INTO song(id,title,duration,liked,totalPlayTime,inLibrary,isLocal,isEpisode) VALUES(?,?,61,?,0,1,?,?)',(song,title,liked,local,episode))
db.execute("INSERT INTO artist(id,name,lastUpdateTime,isLocal,isPodcastChannel) VALUES('artist','Original artist',0,0,0)")
db.execute("INSERT INTO song_artist_map(songId,artistId,position) VALUES('aaaaaaaaaaa','artist',0)")
rows=list(db.execute(sql));assert len(rows)==2
assert rows[0]['artistName']=='Original artist'
assert rows[1]['artistName'] is None
# Query ordering, including an interrupted import whose pre-existing playlist wins,
# is additionally exercised by the Android instrumentation test.
playlist_sql=re.search(r'PLAYLISTS_SQL = "(.*?)"',source)[1]
member_sql=re.search(r'MEMBERS_SQL = "(.*?)"',source)[1]
db.execute("INSERT INTO playlist(id,name,createdAt,lastUpdateTime,isEditable,isLocal,isAutoSync) VALUES('p','My playlist',0,0,1,0,0)")
db.execute("INSERT INTO playlist_song_map(playlistId,songId,position) VALUES('p','aaaaaaaaaaa',1)")
db.execute("INSERT INTO playlist_song_map(playlistId,songId,position) VALUES('p','bbbbbbbbbbb',0)")
assert list(db.execute(playlist_sql))[0]['id']=='p'
assert [r['songId'] for r in db.execute(member_sql,('p',))]==['bbbbbbbbbbb','aaaaaaaaaaa']
print('PASS: v39 SQL projection, missing artists, local/podcast exclusion, playlist ordering')
