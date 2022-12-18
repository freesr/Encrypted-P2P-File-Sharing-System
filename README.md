DESCRIPTION:

The Project is set up using an encrypted P2P paradigm, which includes a Central Index Server to gather meta-data such as Peer ID and the Peer on which the material is stored, as well as the file titles and location. In this paradigm, Peers interact with the Central Index Server to exchange files, search for files, and find out which files are on other Peers and are accessible for download. This paradigm states that all peer-to-peer file transfers must always be done through a direct data connection established by a Socket between the peers sending and receiving the file.

DESIGN :
The P2P file sharing system was created with the P2P architecture and its supporting protocols in mind. Java is utilized to implement the entire design, and Sockets and Threads are two of the abstractions that are employed. The P2P file sharing system has two components 
1. Central Index Server: All of the peers that register with this server get their content indexed. It also offers the peers a search function. The Central Index Server offers the Peers the following Interfaces: 
❖ A peer may use the Registry (peer id, filename) to register its files with the Central Index server. After then, the CIS creates an Index for the peers. 
❖ Search (filename) – this procedure searches the index and gives the requestor a list of all matched peers. 
❖ Create - used to create new file and register these File IPs and attributes in Database

2. Peers: The peer performs both client and server roles. Using "lookup," the user communicates the filename as a client with the indexing server. A list of every other peer that has the file is then returned by the indexing server. The client then establishes a connection with the user's chosen peer and downloads the file. The peer acts as a server, waiting for requests from other peers and sending the requested file in response.


