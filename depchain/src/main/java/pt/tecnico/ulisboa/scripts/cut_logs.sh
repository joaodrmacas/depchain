ROOTDIR=src/main/java/pt/tecnico/ulisboa/logs

head -n 10000 ${ROOTDIR}/server_00.log > ${ROOTDIR}/server_00.tmp && mv ${ROOTDIR}/server_00.tmp ${ROOTDIR}/server_00.log
head -n 10000 ${ROOTDIR}/server_01.log > ${ROOTDIR}/server_01.tmp && mv ${ROOTDIR}/server_01.tmp ${ROOTDIR}/server_01.log
head -n 10000 ${ROOTDIR}/server_02.log > ${ROOTDIR}/server_02.tmp && mv ${ROOTDIR}/server_02.tmp ${ROOTDIR}/server_02.log
head -n 10000 ${ROOTDIR}/server_03.log > ${ROOTDIR}/server_03.tmp && mv ${ROOTDIR}/server_03.tmp ${ROOTDIR}/server_03.log
head -n 10000 ${ROOTDIR}/client_01.log > ${ROOTDIR}/client_01.tmp && mv ${ROOTDIR}/client_01.tmp ${ROOTDIR}/client_01.log
