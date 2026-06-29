moved {
  from = oci_core_vcn.main
  to   = module.network.oci_core_vcn.main
}

moved {
  from = oci_core_internet_gateway.main
  to   = module.network.oci_core_internet_gateway.main
}

moved {
  from = oci_core_route_table.public
  to   = module.network.oci_core_route_table.public
}

moved {
  from = oci_core_route_table.private_db
  to   = module.network.oci_core_route_table.private_db
}

moved {
  from = oci_core_security_list.private_db
  to   = module.network.oci_core_security_list.private_db
}

moved {
  from = oci_core_security_list.empty
  to   = module.network.oci_core_security_list.empty
}

moved {
  from = oci_core_subnet.public_apps
  to   = module.network.oci_core_subnet.public_apps
}

moved {
  from = oci_core_subnet.private_db
  to   = module.network.oci_core_subnet.private_db
}

moved {
  from = oci_core_network_security_group.lb
  to   = module.network.oci_core_network_security_group.lb
}

moved {
  from = oci_core_network_security_group.api
  to   = module.network.oci_core_network_security_group.api
}

moved {
  from = oci_core_network_security_group.bots
  to   = module.network.oci_core_network_security_group.bots
}

moved {
  from = oci_core_network_security_group_security_rule.lb_ingress_http
  to   = module.network.oci_core_network_security_group_security_rule.lb_ingress_http
}

moved {
  from = oci_core_network_security_group_security_rule.lb_egress_to_api
  to   = module.network.oci_core_network_security_group_security_rule.lb_egress_to_api
}

moved {
  from = oci_core_network_security_group_security_rule.api_ingress_from_lb
  to   = module.network.oci_core_network_security_group_security_rule.api_ingress_from_lb
}

moved {
  from = oci_core_network_security_group_security_rule.api_ingress_ssh[0]
  to   = module.network.oci_core_network_security_group_security_rule.api_ingress_ssh
}

moved {
  from = oci_core_network_security_group_security_rule.api_egress_all
  to   = module.network.oci_core_network_security_group_security_rule.api_egress_all
}

moved {
  from = oci_core_network_security_group_security_rule.bots_ingress_ssh[0]
  to   = module.network.oci_core_network_security_group_security_rule.bots_ingress_ssh
}

moved {
  from = oci_core_network_security_group_security_rule.bots_egress_all
  to   = module.network.oci_core_network_security_group_security_rule.bots_egress_all
}

moved {
  from = oci_core_instance.api
  to   = module.compute.oci_core_instance.api
}

moved {
  from = oci_core_instance.bots
  to   = module.compute.oci_core_instance.bots
}

moved {
  from = oci_mysql_mysql_db_system.mysql
  to   = module.mysql.oci_mysql_mysql_db_system.mysql
}

moved {
  from = oci_load_balancer_load_balancer.public
  to   = module.load_balancer.oci_load_balancer_load_balancer.public
}

moved {
  from = oci_load_balancer_backend_set.api
  to   = module.load_balancer.oci_load_balancer_backend_set.api
}

moved {
  from = oci_load_balancer_backend.api
  to   = module.load_balancer.oci_load_balancer_backend.api
}

moved {
  from = oci_load_balancer_listener.http
  to   = module.load_balancer.oci_load_balancer_listener.http
}
