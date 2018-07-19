# Main ONOS UI Web Application

../index.html is the main launch point for the application.

fw/ contains framework related code

view/ contains view related code

Device View -   Shows the registered devices and navigates to/from port, flow, group, meter view
                DeviceDetails view on device row selection
                Added search option based on device id, name, etc.
                Details panel view on row selection of port and flow view

                Navigation to pipeconf view on device view isn't implemented yet
                Editing friendly name into the details panel isn't implemented yet

Host View -     Shows the hosts attached with the registered devices
                HostDetails view on host row selection
                Editing friendly name into the details panel isn't implemented yet

Link View -     Shows the links between the devices

Tunnel View -   Shows the tunnels created between two end-points