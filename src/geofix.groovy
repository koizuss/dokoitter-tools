def cli = new CliBuilder(usage: 'groovy grofix.groovy [options]')

cli.h(longOpt:'help', 'help')
cli.s(longOpt:'server', args:1, argName:'server', 'server [default: localhost]')
cli.p(longOpt:'port', args:1, argName:'port', 'port [default: 5554]')
cli.f(longOpt:'file', args:1, argName:'file', 'data file path [default: empty]')
cli.i(longOpt:'interval', args:1, argName:'interval', 'interval [default: 3000]')

def opt = cli.parse(args)
if(opt.h){ cli.usage(); return }

println 'START'

def server = opt.s ? opt.s : 'localhost'
def port = opt.p ? opt.p : '5554'
def interval = opt.i ? Integer.parseInt(opt.i) : 3000
def lines = opt.f ? new File(opt.f).text.trim().split(/\s/).collect { it.replace(/,/, ' ') } : []
def cancel = false
def execute = {
	def telnet = "telnet $server $port".execute()
	telnet.in.withReader{ reader ->
		telnet.out.withWriter { writer ->
			
			def ok = {
				for(int i = 0; i < 100; i++){
					Thread.sleep 100
					def line = reader.readLine()
					if(line) { println line }
					if(line =~ /^OK/ ){
						return true
					}
				}
				return false
			}
			
			if(ok()){
				while(!cancel){
					for(def data : lines){
						if(data){
							def geofix = "geo fix $data"
							println geofix
							writer.println geofix
							writer.flush()
							
							if(!ok()){ break }
							if(cancel){ break }
							Thread.sleep interval
						}
					}
				}
				if(cancel){ println 'CANCEL' }
			}
			
			writer.println 'exit'
			writer.flush()
			telnet.waitFor()
		}
	}
}

System.in.withReader{ reader ->
	while(true){
		print ">"
		def input = reader.readLine()
		if(input == 'e'){ cancel = true }
		if(input == 'r'){ cancel = false; Thread.start execute }
		if(input == 'q'){ break }
	}
}
println 'END'