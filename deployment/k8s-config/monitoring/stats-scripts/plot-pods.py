
import shutil
import numpy as np
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import time
from pathlib import Path

week_sec=604800
month_sec=30*24*60*60
unix_time=int(time.time())
timespan=unix_time-week_sec


df = pd.read_csv('/home/casteels/stats/pods/podstats.csv')

#users=pd.read_csv('test-users.txt')
users=pd.DataFrame(df['Username'].unique(), columns =['Username']) 

print(users)

#-----------------------------------------------------------------
#Filter the data before plotting:
#-----------------------------------------------------------------

df['Time'] = df[df['Time'] > timespan]['Time']

df=df[df['Username'].isin(users['Username'])]

#df=df[df['Username'].str.contains("casteels")==True]
#df=df[df['Podname'].str.contains("headless")==True]
#df=df[df['Podname'].str.contains("headless")==False]

#-----------------------------------------------------------------
df['Date'] = pd.to_datetime(df['Time'], unit='s')

df['LoadCPU']= df['UsedCPU']/df['ReqCPU']
df['LoadRAM']= df['UsedRAM']/df['ReqRAM']

df['ReqCPU']=df['ReqCPU']/1000
df['UsedCPU']=df['UsedCPU']/1000

df['ReqRAM']=df['ReqRAM']/1000
df['UsedRAM']=df['UsedRAM']/1000

sns.set_theme(style="darkgrid")

#-----------------------------------------------------------------
#plt.title("Requested vs Used CPU Cores for all pods")
#plt.xlabel("Date")
#plt.ylabel("Cores")
#plt.plot(df['Date'], df['ReqCPU'], label='Requested CPU')
#plt.plot(df['Date'], df['UsedCPU'], label='Used CPU')
#plt.legend(fontsize="8", loc ="best")
#plt.xticks(rotation=45)
#plt.savefig("pods-cpu.png",bbox_inches="tight")
#plt.close()

#-----------------------------------------------------------------
#plt.title("Requested vs Used RAM for all pods")
#plt.xlabel("Date")
#plt.ylabel("RAM (GB)")
#plt.plot(df['Date'], df['ReqRAM'], label='Requested RAM')
#plt.plot(df['Date'], df['UsedRAM'], label='Used RAM')
#plt.legend(fontsize="8", loc ="best")
#plt.xticks(rotation=45)
#plt.savefig("pods-ram.png",bbox_inches="tight")
#plt.close()


#-----------------------------------------------------------------
#Totals per time step:
#-----------------------------------------------------------------

ud=df.groupby(['Time']).agg({'UsedCPU':'sum', 'ReqCPU':'sum','UsedRAM':'sum', 'ReqRAM':'sum'})

ud['LoadCPU']= ud['UsedCPU']/ud['ReqCPU']
ud['LoadRAM']= ud['UsedRAM']/ud['ReqRAM']

ud['Time'] = ud.index
ud.index=np.arange(1, len(ud)+1)

#ud['Date'] = pd.to_datetime(ud.iloc[:,0], unit='s')
ud['Date'] = pd.to_datetime(ud['Time'], unit='s')


#-----------------------------------------------------------------

plt.title("Reserved vs Used CPU Cores")
#plt.xlabel("Date")
plt.ylabel("Cores")
plt.plot(ud['Date'], ud['ReqCPU'], label='Reserved CPU', color='red')
plt.plot(ud['Date'], ud['UsedCPU'], label='Used CPU', color='orange')
plt.legend(fontsize="8", loc ="best")
plt.xticks(rotation=45)
plt.savefig("combined-cpu.png",bbox_inches="tight")
plt.close()

#-----------------------------------------------------------------

plt.title("Reserved vs Used RAM")
#plt.xlabel("Date")
plt.ylabel("RAM (GB)")
plt.plot(ud['Date'], ud['ReqRAM'], label='Reserved RAM', color='blue')
plt.plot(ud['Date'], ud['UsedRAM'], label='Used RAM', color='green')
plt.legend(fontsize="8", loc ="best")
plt.xticks(rotation=45)
plt.savefig("combined-ram.png",bbox_inches="tight")
plt.close()


#-----------------------------------------------------------------

plt.title("Resource Utilization")
#plt.xlabel("Date")
plt.ylabel("Load")
plt.plot(ud['Date'], ud['LoadCPU'], label='CPU Load', color='red')
plt.plot(ud['Date'], ud['LoadRAM'], label='RAM Load', color='blue')
plt.legend(fontsize="8", loc ="best")
plt.xticks(rotation=45)
plt.ylim(0,1)
plt.savefig("combined-load.png",bbox_inches="tight")
plt.close()

#-----------------------------------------------------------------
#User plots:
#-----------------------------------------------------------------




for index, row in users.iterrows():
	user=row['Username']
	print(user)

	#uf=df[df['Username'].str.contains(user)==True]
	try:
		uf=df[df['Username'].str.contains(user)==True]
	except:
		continue

	ud=uf.groupby(['Time']).agg({'UsedCPU':'sum', 'ReqCPU':'sum','UsedRAM':'sum', 'ReqRAM':'sum'})

	ud['LoadCPU']= ud['UsedCPU']/ud['ReqCPU']
	ud['LoadRAM']= ud['UsedRAM']/ud['ReqRAM']

	ud['Time'] = ud.index
	ud.index=np.arange(1, len(ud)+1)
	ud['Date'] = pd.to_datetime(ud['Time'], unit='s')

	#ud['Date'] = pd.to_datetime(ud.iloc[:,0], unit='s')


	path='plots/'+user+'-stats/img/'
	Path(path).mkdir(parents=True, exist_ok=True)

	shutil.copyfile('combined-cpu.png', path+'combined-cpu.png')
	shutil.copyfile('combined-ram.png', path+'combined-ram.png')
	shutil.copyfile('combined-load.png', path+'combined-load.png')

	#-----------------------------------------------------------------

	plt.title("Reserved vs Used CPU Cores for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Cores")
	plt.plot(ud['Date'], ud['ReqCPU'], label='Reserved CPU', color='red')
	plt.plot(ud['Date'], ud['UsedCPU'], label='Used CPU', color='orange')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.savefig(path+user+"-cpu.png",bbox_inches="tight")
	plt.close()

	#-----------------------------------------------------------------

	plt.title("Reserved vs Used RAM for "+user)
	#plt.xlabel("Date")
	plt.ylabel("RAM (GB)")
	plt.plot(ud['Date'], ud['ReqRAM'], label='Reserved RAM', color='blue')
	plt.plot(ud['Date'], ud['UsedRAM'], label='Used RAM', color='green')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.savefig(path+user+"-ram.png",bbox_inches="tight")
	plt.close()


	#-----------------------------------------------------------------

	plt.title("Resource Utilization for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Load")
	plt.plot(ud['Date'], ud['LoadCPU'], label='CPU Load', color='red')
	plt.plot(ud['Date'], ud['LoadRAM'], label='RAM Load', color='blue')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.ylim(0,1)
	plt.savefig(path+user+"-load.png",bbox_inches="tight")
	plt.close()



	#-----------------------------------------------------------------

	hf=uf[uf['Podname'].str.contains("headless")==True]

	ud=hf.groupby(['Time']).agg({'UsedCPU':'sum', 'ReqCPU':'sum','UsedRAM':'sum', 'ReqRAM':'sum'})

	ud['LoadCPU']= ud['UsedCPU']/ud['ReqCPU']
	ud['LoadRAM']= ud['UsedRAM']/ud['ReqRAM']
	#ud['Date'] = pd.to_datetime(ud.iloc[:,0], unit='s')
	ud['Time'] = ud.index
	ud.index=np.arange(1, len(ud)+1)
	ud['Date'] = pd.to_datetime(ud['Time'], unit='s')

	#-----------------------------------------------------------------

	plt.title("Headless Reserved vs Used CPU Cores for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Cores")
	plt.plot(ud['Date'], ud['ReqCPU'], label='Reserved CPU', color='red')
	plt.plot(ud['Date'], ud['UsedCPU'], label='Used CPU', color='orange')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.ylim(0,1)
	plt.savefig(path+user+"-headless-cpu.png",bbox_inches="tight")
	plt.close()

	#-----------------------------------------------------------------

	plt.title("Headless Reserved vs Used RAM for "+user)
	#plt.xlabel("Date")
	plt.ylabel("RAM (GB)")
	plt.plot(ud['Date'], ud['ReqRAM'], label='Reserved RAM', color='blue')
	plt.plot(ud['Date'], ud['UsedRAM'], label='Used RAM', color='green')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.savefig(path+user+"-headless-ram.png",bbox_inches="tight")
	plt.close()


	#-----------------------------------------------------------------
	plt.title("Headless Resource Utilization for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Load")
	plt.plot(ud['Date'], ud['LoadCPU'], label='CPU Load', color='red')
	plt.plot(ud['Date'], ud['LoadRAM'], label='RAM Load', color='blue')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.ylim(0,1)
	plt.savefig(path+user+"-headless-load.png",bbox_inches="tight")
	plt.close()


	#-----------------------------------------------------------------

	nf=uf[uf['Podname'].str.contains("headless")==False]

	ud=nf.groupby(['Time']).agg({'UsedCPU':'sum', 'ReqCPU':'sum','UsedRAM':'sum', 'ReqRAM':'sum'})

	ud['LoadCPU']= ud['UsedCPU']/ud['ReqCPU']
	ud['LoadRAM']= ud['UsedRAM']/ud['ReqRAM']
	#ud['Date'] = pd.to_datetime(ud.iloc[:,0], unit='s')
	ud['Time'] = ud.index
	ud.index=np.arange(1, len(ud)+1)
	ud['Date'] = pd.to_datetime(ud['Time'], unit='s')

	#-----------------------------------------------------------------

	plt.title("Headless Reserved vs Used CPU Cores for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Cores")
	plt.plot(ud['Date'], ud['ReqCPU'], label='Reserved CPU', color='red')
	plt.plot(ud['Date'], ud['UsedCPU'], label='Used CPU', color='orange')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.savefig(path+user+"-interactive-cpu.png",bbox_inches="tight")
	plt.close()

	#-----------------------------------------------------------------

	plt.title("Headless Reserved vs Used RAM for "+user)
	#plt.xlabel("Date")
	plt.ylabel("RAM (GB)")
	plt.plot(ud['Date'], ud['ReqRAM'], label='Reserved RAM', color='blue')
	plt.plot(ud['Date'], ud['UsedRAM'], label='Used RAM', color='green')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.savefig(path+user+"-interactive-ram.png",bbox_inches="tight")
	plt.close()


	#-----------------------------------------------------------------
	plt.title("Headless Resource Utilization for "+user)
	#plt.xlabel("Date")
	plt.ylabel("Load")
	plt.plot(ud['Date'], ud['LoadCPU'], label='CPU Load', color='red')
	plt.plot(ud['Date'], ud['LoadRAM'], label='RAM Load', color='blue')
	plt.legend(fontsize="8", loc ="best")
	plt.xticks(rotation=45)
	plt.ylim(0,1)
	plt.savefig(path+user+"-interactive-load.png",bbox_inches="tight")
	plt.close()
