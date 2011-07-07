"""


Python Minefield server
Jack Seibert, Jasen Chan, Will Merrill 2011

Server for multiplayer video game Minefield
Run with at least 1 GB of free RAM

How to Run:
to run: "python server.py"
if that doesnt work: "sudo python server.py"
should work on any system with Python installed (Python is installed by default on Linux and Mac)

Changing the Port:
find the line "PORT = 4000" and change "4000" to whatever port you want



"""

#modules
import SimpleHTTPServer			#contains base class for our custom server
import SocketServer				#allows us to configure the server
import os						#allows us to communicate with the operating system
import random					#gives us many functions related to random numbers, selections, and values
from math import *				#gives us math functions like sqrt(), tan(), cos(), sin(), etc.


os.system('echo "\033]0;Server Log\007"')

PORT = 4000						#the port at which the server will serve

#define global variables, some of which are lists
comments = []
players = []
mines = []
deadPPL = []

lastrequest = None
requestCounter = 0

#custom objects
class Comment:
	"""When a player makes a comment in chat, it is stored on the server in one of these"""
	def __init__(self,poster,content):
		self.name = poster
		self.content = content

class Player:
	"""When a player is logged in on the server, he or she is stored in one of these"""
	def __init__(self,name,position):
		self.name = name
		self.coords = position
		self.velocity = Velocity()
		self.cooldown = 0
		
class Position:
	"""An object for the position of players"""
	def __init__(self,x,y,z):
		self.x = x
		self.y = y
		self.z = z

class Velocity:
	"""An object for the velocities of players"""
	def __init__(self):
		self.x = 0
		self.y = 0
		self.z = 0
		
class Mine:
	"""An object for mines"""
	def __init__(self,x,y,z, dropper):
		self.x = x
		self.y = y
		self.z = z

#The actual serving part of the program!
class Handler(SimpleHTTPServer.SimpleHTTPRequestHandler):

	def do_GET(s):		#When the server receives a GET (download) request
	
		#import global variables
		global players
		global mines
		global deadPPL
		global comments
		global lastrequest
		
		#loop through player array integrally and do stuff for each player
		for j in range(0, len(players)):
			#change cooldown times
			if players[j].cooldown != 0:
				players[j].cooldown -= 1
			#kill players who step on mines
		
		#loop through the player array and do stuff for each player
		for player in players:
		
			#kill players who step on mines, and everyone else in the mines' radii
			for i in range(0, len(mines)):
				if (int(player.coords.x + 10) >= int(mines[i].x - 5) and int(player.coords.x - 10) <= int(mines[i].x) + 5) and (int(player.coords.z + 15) >= int(mines[i].z - 5) and int(player.coords.z - 15) <= int(mines[i].z + 5)):
					mines.pop(i)
					deadPPL.append(player)
					for p in range(len(players)):
						if players[p] is player:
							ind = p
					players.pop(ind)
					#blast radius
					"""
					for otherPlayer in players:
						if player.name != otherPlayer.name:
							if int(player.coords.x) + 50 > otherPlayer.coords.x > int(player.coords.x) - 50 and int(player.coords.y) + 50 > otherPlayer.coords.y > int(player.coords.y) -50:
								if not player.name in deadPPL:
									deadPPL.append(otherPlayer)
									for x in range(0,len(players)):
										if players[x].name == parts[1]:
											players.pop(x)
					"""
											
			#change each player's position by his or her velocity
			player.coords.x += player.velocity.x
			player.coords.y += player.velocity.y
			player.coords.z += player.velocity.z
			
			#make people fall if they jump
			if player.coords.y > 0:
				player.velocity.y -= .01
			if player.coords.y <= 0:
				player.velocity.y = 0
				player.coords.y = 0
			
			"""
			#make people bounce off each other
			for op in players:
				if not op is player:
					if int(player.coords.x + 10) >= int(op.coords.x - 10) and int(op.coords.x + 10) >= int(player.coords.x - 10):
						if player.velocity.x != 0:
							op.velocity.x += player.velocity.x * (player.velocity.x / abs(player.velocity.x))
					if int(player.coords.z + 15) >= int(op.coords.z - 15) and int(player.coords.z - 15) <= int(op.coords.z + 15):
						if player.velocity.z != 0:
							op.velocity.z += player.velocity.z * (player.velocity.z / abs(player.velocity.z))
			"""
						
			#make people bounce off walls
			if abs(player.coords.x) >= 390:
				player.velocity.x = - player.velocity.x
			elif abs(player.coords.z) >= 285:
				player.velocity.z = - player.velocity.z
			player.velocity.x -= player.velocity.x / 150
			player.velocity.z -= player.velocity.z / 150
		
		global requestCounter
		requestCounter += 1
		print "Client request query: " + s.path
		
		#analyze various requests sent from the clients
		if s.path.endswith(".gameinfo"):		#request to download rules files
			s.wfile.write("Game Info File")
			s.end_headers()
			f = open(os.getcwd() + s.path,"r")
			for line in f.readlines():
				s.wfile.write(line)
			f.close()
		else:
			parts = s.path[1:].split("|")		#parse the request into a list called parts
			if requestCounter % 2 == 1:
				motion = [False,False,False]
				
				if parts[0] == "login" and not parts[1] in map(lambda x: x.name,players) and not parts[1] in map(lambda x: x.name,deadPPL):		#a login request
					x = random.random() * 200
					z = random.random() * 200
					players.append(Player(parts[1],Position(x,0,z)))
					print parts[1] + " has logged in"
				
				elif parts[0] == "logout":			#a logout request
					for x in range(0,len(players)):
						if players[x].name == parts[1]:
							players.pop(x)
							print parts[1] + " has logged out"
				
				elif parts[0] == "comment":			#a request to post a comment
					if parts[1] in map(lambda x: x.name,players):
						comments.append(Comment(parts[1],parts[2]))
				
				elif parts[0] == "up":			#a request to move up
					motion = [True,0,0,.1]
				
				elif parts[0] == "down":			#a request to move down
					motion = [True,0,0,-.1]
				
				elif parts[0] == "left":			#a request to move to the left
					motion = [True,-.1,0,0]
					
				elif parts[0] == "right":			#a request to move to the right
					motion = [True,.1,0,0]
					
				elif parts[0] == "jump":			#a request to jump
					motion = [True,0,.1,0]
					
				elif parts[0] == "renderdata":		#a request for data to be returned from the server
					if parts[1] == "positions":		#player positions
						l = map(lambda x: str(x.name) + ":" + str(x.coords.x) + "," + str(x.coords.y) + "," + str(x.coords.z) + ";",players)
						s.wfile.write("".join(l))
					elif parts[1] == "comments":	#comment log
						l = map(lambda x: x.name + ":" + x.content + "\n",comments)
						s.wfile.write("".join(l))
					elif parts[1] == "mines":		#mine positions
						l = map(lambda x: str(x.x) + "," + str(x.y) + "," + str(x.z) + ";", mines)
						s.wfile.write("".join(l))
					elif parts[1] == "dead":		#dead players
						l = map(lambda x: x.name + ";", deadPPL)
						print "".join(l)
						s.wfile.write("".join(l))
					elif parts[1] == "cooldown":	#player's cooldown times
						for player in players:
							if player.name == parts[2]:
								s.wfile.write(player.cooldown)
								break
				
				elif parts[0] == "mine":			#a request to lay a mine
					dropper = parts[1]
					for i in range(0, len(players)):
						player = players[i]
						if player.name == dropper and player.cooldown <= 0:
							mines.append(Mine(player.coords.x - sin(radians(atan(player.velocity.x / player.velocity.z))) * 25, player.coords.y, player.coords.z - cos(radians(atan(player.velocity.x / player.velocity.z))) * 25, dropper))
							players[i].cooldown = len(players) * 500
							
				if motion[0]:			#process motion data from above, and move players
					for player in players:
							if player.name == parts[1]:
								player.velocity.x += motion[1]
								player.velocity.y += motion[2]
								player.velocity.z += motion[3]
				
				#output in the server log
				print "Players:", map(lambda x: x.name,players)
			
				print "\nCOMMENT LOG ------------------"
				for comment in comments:
					print comment.name + " writes: " + comment.content
				
				#print map(lambda x: x.velocity.x, players)
				
				
				print "\n\n\n"
			
			lastrequest = s.path


#initialize the server 
httpd = SocketServer.TCPServer(("", PORT), Handler)
print "serving at port", PORT
try:
	httpd.serve_forever()		#let the server serve in an infinite loop
except:
	print "There was an error"